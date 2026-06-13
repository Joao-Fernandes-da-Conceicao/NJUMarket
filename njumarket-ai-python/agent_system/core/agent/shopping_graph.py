"""
显式 LangGraph 编排：路由 → 约束提取 → 向量/混合检索 → 筛选 → 反思(reflection) → 终局回复。
非购物类走单节点直出，不经过检索链路。

约束优先级架构：
  P0（硬约束，来自当前消息，不可违背）：价格、品类、成色、品牌要求/排除、地区
  P1（软偏好，来自用户历史画像，仅在满足 P0 后参考）：品牌偏好、风格偏好等
"""
from __future__ import annotations

import logging
import re
from typing import Annotated, Any, Literal, TypedDict

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages
from pydantic import BaseModel, Field

from agent_system.core.llm.client import get_llm_client
from agent_system.core.tools.commodity_toolkit import CommodityToolkit

log = logging.getLogger(__name__)

# 演示模式：减少迭代，尽快返回结果
MAX_RETRIEVE_STEPS = 1
MAX_FILTER_STEPS = 1


class AgentGraphState(TypedDict, total=False):
    messages: Annotated[list[BaseMessage], add_messages]
    user_message: str
    intent: Literal["shopping", "general"]
    retrieval_mode: Literal["vector", "hybrid"]
    search_query: str
    location: str
    candidates: list[dict[str, Any]]
    candidates_text: str
    filtered_ids: list[str]
    filter_rationale: str
    reflection_action: Literal["accept", "refilter", "reretrieve"]
    reflection_notes: str
    refined_query: str
    retrieve_attempts: int
    filter_attempts: int
    final_reply: str
    # P0 硬约束（从当前消息提取，贯穿全图）
    price_min: float | None
    price_max: float | None
    required_category: str
    required_condition: str
    required_brands: list[str]
    excluded_brands: list[str]
    soft_notes: str      # P1 软偏好（来自当前消息的风格性描述，非硬约束）


class RouterOutput(BaseModel):
    intent: Literal["shopping", "general"] = Field(
        description="shopping：找货、推荐、比价、有没有、预算/成色/型号等；general：与商品检索无关"
    )
    retrieval_mode: Literal["vector", "hybrid"] = Field(
        description="hybrid：先关键词再融合语义（默认，贴近「按需求搜商品」）；vector：纯口语/抽象需求"
    )
    search_query: str = Field(
        description="提炼出的检索用语：品牌/型号/品类/关键属性/预算等，用于替代用户自己反复改搜索词"
    )
    location: str = Field(default="", description="地区，无则空")


class ConstraintOutput(BaseModel):
    """从用户当前消息中结构化提取的硬约束，优先级高于一切历史画像。"""
    price_max: float | None = Field(
        default=None,
        description="最高价格/预算上限（元）。「5000以内」→5000，「不超过3000」→3000，无则null"
    )
    price_min: float | None = Field(
        default=None,
        description="最低价格（元）。「5000以上」→5000，无则null"
    )
    required_category: str = Field(
        default="",
        description="明确品类，如「笔记本」「手机」「相机」，无则空"
    )
    required_condition: str = Field(
        default="",
        description="成色要求，如「全新」「九成新」「8成新以上」，无则空"
    )
    required_brands: list[str] = Field(
        default_factory=list,
        description="必须是某品牌，如「只要苹果」→['苹果']，无则[]"
    )
    excluded_brands: list[str] = Field(
        default_factory=list,
        description="明确排除的品牌，如「不要联想」→['联想']，无则[]"
    )
    soft_notes: str = Field(
        default="",
        description="偏好性描述（非硬约束），如「轻薄优先」「外观好看」「性能强」，无则空"
    )


class FilterOutput(BaseModel):
    commodity_ids: list[str] = Field(
        description="仅从候选中选 commodityId，按「更贴用户需求→更高性价比→信息更充分」综合优先级排序，最多10个"
    )
    rationale: str = Field(description="一两句话说明排序依据（匹配点、价格合理性、描述是否足以决策）")


class ReflectionOutput(BaseModel):
    action: Literal["accept", "refilter", "reretrieve"] = Field(
        description="accept 可回复用户；refilter 在同一批候选上重筛；reretrieve 用新查询重检索"
    )
    notes: str = Field(description="对照用户意图的反思：是否偏题、价位/类目是否合适等")
    refined_query: str = Field(default="", description="仅当 reretrieve 时填写改进后的检索语句")


def _last_user_text(messages: list[BaseMessage], fallback: str) -> str:
    for m in reversed(messages or []):
        if isinstance(m, HumanMessage):
            c = m.content
            return c if isinstance(c, str) else str(c)
    return fallback


def _hard_filter_candidates(
    candidates: list[dict[str, Any]],
    state: "AgentGraphState",
) -> list[dict[str, Any]]:
    """
    在候选进入 LLM 之前执行硬过滤，剔除不满足 P0 约束的商品。
    价格为 0/缺失的商品（数据不全）保留，让 LLM 自行判断。
    品牌/品类/成色过滤仅做简单字符串匹配（title+description），避免误杀。
    """
    price_min: float | None = state.get("price_min")
    price_max: float | None = state.get("price_max")
    required_brands: list[str] = state.get("required_brands") or []
    excluded_brands: list[str] = state.get("excluded_brands") or []

    result = []
    for c in candidates:
        title = (str(c.get("title") or "") + " " + str(c.get("description") or "")).lower()

        # 价格过滤
        try:
            p = float(c.get("price") or 0)
        except (TypeError, ValueError):
            p = 0.0
        if p > 0:
            if price_min is not None and p < price_min:
                continue
            if price_max is not None and p > price_max:
                continue

        # 排除品牌
        if any(b.lower() in title for b in excluded_brands):
            continue

        # 必须品牌（只要 title 含其中之一即可）
        if required_brands and not any(b.lower() in title for b in required_brands):
            continue

        result.append(c)
    return result


def _build_p0_section(state: "AgentGraphState") -> str:
    """
    构建 P0 硬约束说明块，注入到 filter/reflect/respond 节点的 prompt 中。
    用于让 LLM 清楚知道哪些约束来自当前消息、绝对不可违背。
    """
    lines: list[str] = []
    price_max = state.get("price_max")
    price_min = state.get("price_min")
    if price_max is not None and price_min is not None:
        lines.append(f"• 价格区间：¥{price_min:.0f}～¥{price_max:.0f}（超出范围一律排除）")
    elif price_max is not None:
        lines.append(f"• 价格上限：¥{price_max:.0f}（超出此价格的商品一律不选，不得例外）")
    elif price_min is not None:
        lines.append(f"• 价格下限：¥{price_min:.0f}（低于此价格的商品一律不选）")

    cat = (state.get("required_category") or "").strip()
    if cat:
        lines.append(f"• 品类：必须是「{cat}」，其他品类不推荐")

    cond = (state.get("required_condition") or "").strip()
    if cond:
        lines.append(f"• 成色：必须满足「{cond}」")

    for b in (state.get("required_brands") or []):
        lines.append(f"• 必须是品牌：{b}")
    for b in (state.get("excluded_brands") or []):
        lines.append(f"• 排除品牌：{b}")

    if not lines:
        return ""

    return (
        "【P0 当前指令约束（来自用户本条消息，绝对优先，不得以任何理由绕过）】\n"
        + "\n".join(lines)
        + "\n"
        "以上约束高于用户历史画像、历史偏好及任何 P1 软性偏好。\n"
    )


def _format_candidates_text(candidates: list[dict[str, Any]], max_lines: int = 40) -> str:
    lines: list[str] = []
    for i, c in enumerate((candidates or [])[:max_lines], 1):
        cid = str(c.get("commodityId") or "")
        title = str(c.get("title") or "")
        price = c.get("price")
        try:
            p = float(price) if price is not None else 0.0
        except (TypeError, ValueError):
            p = 0.0
        src = str(c.get("source") or "")
        desc = str(c.get("description") or "").strip().replace("\n", " ")
        if len(desc) > 120:
            desc = desc[:120] + "…"
        base = f"{i}. {cid} | {title} | ¥{p:.2f} | {src}"
        lines.append(base + (f" | 简介:{desc}" if desc else ""))
    if not lines:
        return "（无候选）"
    return "\n".join(lines)


def _reply_text(msg: AIMessage | Any) -> str:
    c = msg.content if hasattr(msg, "content") else msg
    return c if isinstance(c, str) else str(c)


def _stream_chunks_to_text(stream_iter: Any) -> str:
    """用 LangChain 原生 llm.stream 聚合为完整文本（供 astream_events 捕获 token）。"""
    parts: list[str] = []
    for chunk in stream_iter:
        c = getattr(chunk, "content", None)
        if not c:
            continue
        if isinstance(c, str):
            parts.append(c)
        elif isinstance(c, list):
            for block in c:
                if isinstance(block, dict) and block.get("type") == "text":
                    parts.append(str(block.get("text") or ""))
    return "".join(parts)


def build_shopping_graph(toolkit: CommodityToolkit):
    llm = get_llm_client()
    router_llm = llm.with_structured_output(RouterOutput)
    constraint_llm = llm.with_structured_output(ConstraintOutput)
    filter_llm = llm.with_structured_output(FilterOutput)
    reflect_llm = llm.with_structured_output(ReflectionOutput)

    def node_router(state: AgentGraphState) -> dict[str, Any]:
        """意图路由 + P0 约束提取（两次独立结构化调用，职责清晰）。"""
        msgs = state.get("messages") or []
        um = (state.get("user_message") or "").strip() or _last_user_text(msgs, "")

        # 第一步：路由意图
        route_prompt = (
            "你是检索路由模块：判断用户是否在「买/找/比二手商品」，并产出用于后台搜索的关键词。\n"
            f"用户消息：{um}\n\n"
            "意图：找商品、推荐、比价、预算、成色、型号、有没有卖、类似××、哪家便宜等 → shopping；"
            "纯闲聊、与商品无关 → general。\n"
            "search_query：把检索核心抽出（品牌/型号/品类/关键属性），帮用户写好搜索词。\n"
            "retrieval_mode：默认 hybrid；纯口语/抽象需求用 vector。"
        )
        route_out = router_llm.invoke([HumanMessage(content=route_prompt)])
        log.info("[router] intent=%s mode=%s query=%r", route_out.intent, route_out.retrieval_mode, route_out.search_query)

        # 第二步：P0 约束提取（仅当 shopping 时才有意义，但提取无害）
        constraint_prompt = (
            "从下面这条用户消息中，提取所有明确的购物硬约束（P0）。\n"
            "硬约束 = 用户明确说了「必须/不超过/只要/排除/以内/以上/全新/成色」等，"
            "不包含模糊偏好（如「希望轻薄」「喜欢ROG」属于 soft_notes）。\n"
            f"用户消息：{um}"
        )
        c_out = constraint_llm.invoke([HumanMessage(content=constraint_prompt)])
        log.info(
            "[router] P0 constraints: price=(%s, %s) category=%r condition=%r "
            "required_brands=%s excluded_brands=%s soft=%r",
            c_out.price_min, c_out.price_max,
            c_out.required_category, c_out.required_condition,
            c_out.required_brands, c_out.excluded_brands, c_out.soft_notes,
        )

        return {
            "intent": route_out.intent,
            "retrieval_mode": route_out.retrieval_mode,
            "search_query": (route_out.search_query or um).strip(),
            "location": (route_out.location or "").strip(),
            # P0 硬约束写入 state，贯穿全图
            "price_max": c_out.price_max,
            "price_min": c_out.price_min,
            "required_category": c_out.required_category or "",
            "required_condition": c_out.required_condition or "",
            "required_brands": c_out.required_brands or [],
            "excluded_brands": c_out.excluded_brands or [],
            "soft_notes": c_out.soft_notes or "",
        }

    def node_retrieve(state: AgentGraphState) -> dict[str, Any]:
        mode = state.get("retrieval_mode") or "hybrid"
        base_q = (state.get("search_query") or "").strip()
        refined = (state.get("refined_query") or "").strip()
        q = refined or base_q
        loc = state.get("location") or ""
        attempts = int(state.get("retrieve_attempts") or 0) + 1
        prev_fa = int(state.get("filter_attempts") or 0)
        filter_attempts = 0 if refined else prev_fa

        raw_candidates = toolkit.retrieve_candidates(mode, q, loc, 20)
        # 硬过滤：价格/品牌约束在进入 LLM 前就剔除，不依赖 LLM 注意力
        candidates = _hard_filter_candidates(raw_candidates, state)
        log.info("[retrieve] query=%r raw=%d after_hard_filter=%d", q, len(raw_candidates), len(candidates))
        return {
            "candidates": candidates,
            "candidates_text": _format_candidates_text(candidates),
            "retrieve_attempts": attempts,
            "filter_attempts": filter_attempts,
            "refined_query": "",
        }

    def node_filter(state: AgentGraphState) -> dict[str, Any]:
        msgs = state.get("messages") or []
        um = (state.get("user_message") or "").strip() or _last_user_text(msgs, "")
        attempts = int(state.get("filter_attempts") or 0) + 1
        ctx = state.get("candidates_text") or ""
        p0 = _build_p0_section(state)
        soft = (state.get("soft_notes") or "").strip()
        p1_hint = f"【P1 软偏好（满足P0后参考）】{soft}\n" if soft else ""
        prompt = (
            "你是「替用户货比三家」的筛选器，目标是挑出更值得用户点开的少数商品。\n"
            "【演示模式】时间优先，有一两个大致匹配的即可，不必精挑细选。\n"
            f"{p0}"
            f"{p1_hint}"
            "筛选优先级（在满足P0约束的前提下）：\n"
            "1）与用户诉求一致（品类、配置、P1偏好等）；\n"
            "2）性价比：价格相对成色/配置是否合理；\n"
            "3）可决策性：描述是否具体可信。\n"
            "仅使用候选里出现的 commodityId，最多 10 个，按推荐顺序排列。\n\n"
            f"候选：\n{ctx}\n\n用户诉求：{um}\n"
            "若候选整体跑偏或质量差，可返回空列表并在 rationale 说明原因。"
        )
        out = filter_llm.invoke([HumanMessage(content=prompt)])
        ids = [str(x).strip() for x in (out.commodity_ids or []) if str(x).strip()]
        log.info("[filter] attempt=%d selected=%d ids=%s rationale=%r", attempts, len(ids), ids, out.rationale)
        return {
            "filtered_ids": ids,
            "filter_rationale": out.rationale or "",
            "filter_attempts": attempts,
        }

    def node_reflect(state: AgentGraphState) -> dict[str, Any]:
        msgs = state.get("messages") or []
        um = (state.get("user_message") or "").strip() or _last_user_text(msgs, "")
        ctx = (state.get("candidates_text") or "")[:4000]
        ids = state.get("filtered_ids") or []
        rat = state.get("filter_rationale") or ""
        p0 = _build_p0_section(state)
        prompt = (
            "反思：当前结果是否可以回复用户？【演示模式：差不多就 accept，不反复优化】\n"
            f"{p0}"
            "【P0 违规检查】若当前已选商品中有任何违反上述P0约束的，必须 refilter。\n\n"
            "只要有一两个大致匹配、价格在范围内的商品就 accept。"
            "除非候选严重偏题（品类/价位全错），否则不要 reretrieve。\n"
            f"用户诉求：{um}\n\n候选摘要：\n{ctx}\n\n"
            f"当前选出：{ids}\n筛选理由：{rat}\n\n"
            "action：accept / refilter / reretrieve。通常 accept 即可。"
        )
        out = reflect_llm.invoke([HumanMessage(content=prompt)])
        log.info("[reflect] action=%s refined_query=%r notes=%r", out.action, out.refined_query, out.notes)
        return {
            "reflection_action": out.action,
            "reflection_notes": out.notes or "",
            "refined_query": (out.refined_query or "").strip(),
        }

    def node_respond_general(state: AgentGraphState) -> dict[str, Any]:
        msgs = list(state.get("messages") or [])
        text = _stream_chunks_to_text(llm.stream(msgs))
        t = text.strip()
        return {"final_reply": t, "messages": [AIMessage(content=t)]}

    def node_respond_shopping(state: AgentGraphState) -> dict[str, Any]:
        msgs = state.get("messages") or []
        um = (state.get("user_message") or "").strip() or _last_user_text(msgs, "")
        ids = list(state.get("filtered_ids") or [])
        toolkit.set_recommended_commodities(ids)
        cand = state.get("candidates") or []
        by_id = {str(c.get("commodityId")): c for c in cand if c.get("commodityId")}
        detail_lines = []
        for cid in ids[:10]:
            c = by_id.get(cid)
            if c:
                title = str(c.get("title") or "")
                desc = str(c.get("description") or "").strip().replace("\n", " ")
                price = c.get("price")
                try:
                    price_str = f"¥{float(price):.0f}" if price else ""
                except (TypeError, ValueError):
                    price_str = ""
                line = f"- {title}{f' {price_str}' if price_str else ''}"
                if desc:
                    line += f" | {desc[:80]}{'…' if len(desc) > 80 else ''}"
                detail_lines.append(line)
            else:
                detail_lines.append(f"- (商品详情待加载)")
        notes = state.get("reflection_notes") or ""
        rat = state.get("filter_rationale") or ""
        p0 = _build_p0_section(state)
        prompt = (
            "用自然、友好的中文写最终回复。你的定位是帮用户完成「搜—比—选」里「比」和「选」的部分。\n"
            f"{p0}"
            f"用户诉求：{um}\n"
            f"已推荐商品（按优先级，共{len(ids)}件）：\n" + "\n".join(detail_lines or ["（无符合条件的商品）"]) + "\n"
            f"筛选理由：{rat}\n反思备注：{notes}\n\n"
            "写作要求：\n"
            "- 有推荐时：用1-2句说明为何这些商品值得看（符合需求/性价比/描述可信），"
            "  无需逐条罗列，整体概括即可。\n"
            "- 如有P0约束，可在回复里自然提及（如「5000以内」），但不要自行虚构或修改约束范围。\n"
            "- 【严格禁止】回复中不得出现商品ID、commodityId、[商品卡片]等字样——"
            "  卡片由前端渲染，你只写纯自然语言。\n"
            "- 无推荐时：诚实说明，给出下一步建议（换词/放宽预算/放宽地区）。"
        )
        text = _stream_chunks_to_text(llm.stream([HumanMessage(content=prompt)]))
        t = text.strip()
        log.info("[respond_shopping] reply_len=%d recommended=%d", len(t), len(ids))
        return {"final_reply": t, "messages": [AIMessage(content=t)]}

    def route_after_router(state: AgentGraphState) -> str:
        return "retrieve" if state.get("intent") == "shopping" else "respond_general"

    def route_after_retrieve(state: AgentGraphState) -> str:
        if not state.get("candidates"):
            return "respond_shopping"
        return "filter"

    def route_after_reflect(state: AgentGraphState) -> str:
        action = state.get("reflection_action") or "accept"
        ra = int(state.get("retrieve_attempts") or 0)
        fa = int(state.get("filter_attempts") or 0)
        if action == "accept":
            return "respond_shopping"
        if action == "reretrieve" and ra < MAX_RETRIEVE_STEPS:
            return "retrieve"
        if action == "refilter" and fa < MAX_FILTER_STEPS:
            return "filter"
        return "respond_shopping"

    g = StateGraph(AgentGraphState)
    g.add_node("router", node_router)
    g.add_node("retrieve", node_retrieve)
    g.add_node("filter", node_filter)
    g.add_node("reflect", node_reflect)
    g.add_node("respond_general", node_respond_general)
    g.add_node("respond_shopping", node_respond_shopping)

    g.add_edge(START, "router")
    g.add_conditional_edges("router", route_after_router)
    g.add_conditional_edges("retrieve", route_after_retrieve)
    g.add_edge("filter", "reflect")
    g.add_conditional_edges("reflect", route_after_reflect)
    g.add_edge("respond_general", END)
    g.add_edge("respond_shopping", END)

    return g.compile()
