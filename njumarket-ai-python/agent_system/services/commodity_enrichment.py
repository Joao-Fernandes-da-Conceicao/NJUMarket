"""商品检索丰度增强：与 Java CommodityEnrichmentService 对齐。"""
from __future__ import annotations

import logging

from langchain_core.messages import HumanMessage

from agent_system.core.llm.client import get_llm_client

log = logging.getLogger(__name__)

ENRICHMENT_SYSTEM = (
    "你是一个二手商品检索优化助手。"
    "根据给定的商品信息，生成一段「仅用于搜索引擎检索」的丰度文本。要求：\n"
    "1. 保留原标题与描述中的关键词；\n"
    "2. 补充同义词、常见说法、使用场景（例如：教材→课本、考研书、教科书）；\n"
    "3. 若有成色、地区、品类信息，用自然词融入；\n"
    "4. 输出为一段连贯的短文，不要列表、不要编号，总长度 80～200 字；\n"
    "5. 只输出这段文本，不要任何前缀、解释或换行。"
)


def enrich_for_search(
    title: str,
    description: str | None,
    category: str | None,
    condition_level: str | None,
    location: str | None,
    address_snapshot_full: str | None,
) -> str | None:
    if not (title or "").strip():
        return None
    parts: list[str] = [f"商品标题：{title.strip()}"]
    if description and str(description).strip():
        desc = str(description).strip()
        if len(desc) > 500:
            desc = desc[:500] + "…"
        parts.append(f"商品描述：{desc}")
    if category and str(category).strip():
        parts.append(f"品类：{category.strip()}")
    if condition_level and str(condition_level).strip():
        parts.append(f"成色：{condition_level.strip()}")
    if location and str(location).strip():
        parts.append(f"地区/位置：{location.strip()}")
    if address_snapshot_full and str(address_snapshot_full).strip():
        parts.append(f"地址：{address_snapshot_full.strip()}")
    parts.append("\n请生成上述商品的检索丰度文本。")
    user_content = "\n".join(parts)
    full_prompt = ENRICHMENT_SYSTEM + "\n\n" + user_content
    try:
        log.debug("enrich_for_search: title=%r, category=%r", title, category)
        llm = get_llm_client(temperature=0.5)
        out = llm.invoke([HumanMessage(content=full_prompt)])
        text = out.content if isinstance(out.content, str) else str(out.content)
        text = (text or "").strip()
        if text:
            log.debug("enrich_for_search OK: %d chars", len(text))
        return text if text else None
    except Exception as exc:
        log.warning("enrich_for_search failed: %s", exc)
        return None
