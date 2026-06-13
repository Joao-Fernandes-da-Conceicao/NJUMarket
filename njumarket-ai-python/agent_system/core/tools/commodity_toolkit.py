from __future__ import annotations

from collections import defaultdict
from typing import Any, Literal

from langchain_core.tools import BaseTool, tool

from agent_system.core.tools.search_tool import SearchTool
from agent_system.integrations.commodity_internal import fetch_commodities_batch
from agent_system.rag.retriever import retrieve_commodity_docs


def _commodities_from_es_result(es: Any) -> list[dict[str, Any]]:
    if isinstance(es, dict) and es.get("success") and isinstance(es.get("data"), dict):
        return list(es["data"].get("commodities") or [])
    return []


def _enrich_candidates_with_batch(merged: dict[str, dict[str, Any]]) -> dict[str, dict[str, Any]]:
    """用 internal 批量接口补全 title/price/description，保留检索来源 source（es / vector）。"""
    if not merged:
        return merged
    ids = [k for k in merged if k]
    details = fetch_commodities_batch(ids)
    out: dict[str, dict[str, Any]] = {}
    for cid, row in merged.items():
        retrieval_src = row.get("source", "")
        d = details.get(cid)
        if d:
            enriched = {**d, "commodityId": cid}
            enriched["source"] = retrieval_src
            out[cid] = enriched
        else:
            out[cid] = row
    return out


class CommodityToolkit:
    """
    LangChain 范式下的“工具集合”：
    - 直接向 ReAct 注册一组工具
    - 不引入 skill 插件抽象
    """

    def __init__(self) -> None:
        self._search = SearchTool()
        self._current_conversation_id: str = "default"
        self._recommended_ids_by_cid: dict[str, list[str]] = defaultdict(list)
        self._recommended_items_by_cid: dict[str, list[dict[str, Any]]] = defaultdict(list)

    def begin_turn(self, conversation_id: str | None) -> None:
        cid = conversation_id or "default"
        self._current_conversation_id = cid
        self._recommended_ids_by_cid[cid] = []
        self._recommended_items_by_cid[cid] = []

    def end_turn(self, conversation_id: str | None) -> None:
        return None

    def system_prompt_section(self) -> str:
        return (
            "你是南大集市（校园二手交易）的智能购物助手。\n"
            "核心价值：把「用户自己搜关键词、点开多条商品、人工货比三家」压缩成一步——"
            "系统先按用户所需的核心检索词（必要时结合语义）拉取候选，再在候选上做综合评估与排序，"
            "让用户直接看到更贴需求、更值得点的商品。\n"
            "评估时优先兼顾：①与用户需求的一致性；②性价比（价格相对成色/配置的合理性）；"
            "③信息可信度（标题与描述是否足够具体、可决策）。在二手场景下，描述翔实往往比「只有好听的标题」更可靠。\n"
            "购物类问题由后台图（检索→筛选→反思）完成后再生成回复；非购物类可自然对话。"
            "用户画像与历史摘要见下文系统增广。"
        )

    def get_recommended_ids(self, conversation_id: str | None) -> list[str]:
        return list(self._recommended_ids_by_cid.get(conversation_id or "default", []))

    def get_recommended_items(self, conversation_id: str | None) -> list[dict[str, Any]]:
        return list(self._recommended_items_by_cid.get(conversation_id or "default", []))

    def set_recommended_commodities(self, commodity_ids: list[str]) -> None:
        """程序化写入本轮推荐 ID（与 confirmRecommendedCommodities 一致）。"""
        cid = self._current_conversation_id or "default"
        selected = [str(x) for x in (commodity_ids or []) if x]
        self._recommended_ids_by_cid[cid] = selected
        details = fetch_commodities_batch(selected)
        items: list[dict[str, Any]] = []
        for x in selected:
            d = details.get(x)
            if d:
                items.append({**d, "commodityId": x})
            else:
                items.append({"commodityId": x})
        self._recommended_items_by_cid[cid] = items

    def retrieve_candidates(
        self,
        mode: Literal["vector", "hybrid"],
        query: str,
        location: str = "",
        limit: int = 20,
    ) -> list[dict[str, Any]]:
        """
        LangGraph 节点用：向量或混合检索，返回统一结构（含 commodityId、title、price、source）。
        hybrid = ES 关键词 + Milvus 向量去重合并。
        """
        q = (query or "").strip()
        if not q:
            return []
        merged: dict[str, dict[str, Any]] = {}
        if mode == "hybrid":
            es = self._search.run(query=q, location=location or "", limit=limit)
            for c in _commodities_from_es_result(es):
                cid = str(c.get("commodityId") or "").strip()
                if cid:
                    merged[cid] = {**c, "commodityId": cid, "source": "es"}
            for h in retrieve_commodity_docs(q, limit):
                iid = str((h or {}).get("id") or "").strip()
                if iid and iid not in merged:
                    merged[iid] = {
                        "commodityId": iid,
                        "title": "(向量检索)",
                        "price": 0,
                        "source": "vector",
                    }
            return list(_enrich_candidates_with_batch(merged).values())
        for h in retrieve_commodity_docs(q, limit):
            iid = str((h or {}).get("id") or "").strip()
            if iid:
                merged[iid] = {
                    "commodityId": iid,
                    "title": "(向量检索)",
                    "price": 0,
                    "source": "vector",
                }
        return list(_enrich_candidates_with_batch(merged).values())

    def get_tools(self) -> list[BaseTool]:
        @tool("searchCommodities")
        def searchCommodities(query: str, location: str = "", limit: int = 20) -> str:
            es = self._search.run(query=query, location=location, limit=limit)
            commodities = _commodities_from_es_result(es)
            lines = [f"找到 {len(commodities)} 个相关商品："]
            for i, c in enumerate(commodities[:15], 1):
                cid = c.get("commodityId")
                title = c.get("title") or ""
                price = c.get("price") or 0
                lines.append(f"{i}. [commodityId={cid}] {title} - ¥{float(price):.2f}")
            lines.append(
                "（用户画像见系统 prompt；请结合需求做「匹配度+性价比+描述可信度」筛选，"
                "再调用 confirmRecommendedCommodities 确认最终推荐列表。）"
            )
            return "\n".join(lines)

        @tool("searchCommoditiesByVector")
        def searchCommoditiesByVector(query: str, limit: int = 20) -> str:
            hits = retrieve_commodity_docs(query, limit)
            ids = [str(h.get("id")) for h in hits if isinstance(h, dict) and h.get("id")]
            dm = fetch_commodities_batch(ids)
            lines = [f"向量检索命中 {len(ids)} 条："]
            for i, vid in enumerate(ids[:20], 1):
                c = dm.get(vid)
                title = (c.get("title") if c else None) or "(暂无详情，仅 ID)"
                lines.append(f"{i}. [{vid}] {title}")
            return "\n".join(lines)

        @tool("searchCommoditiesHybrid")
        def searchCommoditiesHybrid(query: str, location: str = "", limit: int = 20) -> str:
            es_text = searchCommodities(query=query, location=location, limit=limit)
            vec_text = searchCommoditiesByVector(query=query, limit=limit)
            return f"{es_text}\n（补充）{vec_text}"

        @tool("confirmRecommendedCommodities")
        def confirmRecommendedCommodities(commodityIds: list[str]) -> str:
            self.set_recommended_commodities(list(commodityIds or []))
            return "已确认推荐商品列表。"

        return [searchCommodities, searchCommoditiesByVector, searchCommoditiesHybrid, confirmRecommendedCommodities]

