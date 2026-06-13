"""商品服务 internal 批量查询（与 Java CommodityClient / AI 消息列表补全一致）。"""
from __future__ import annotations

from typing import Any

import httpx

from agent_system.config.settings import get_settings


def fetch_commodities_batch(ids: list[str]) -> dict[str, dict[str, Any]]:
    """POST /api/internal/commodities/batch，返回 commodityId -> 商品 DTO。"""
    if not ids:
        return {}
    base = get_settings().commodity_base_url.rstrip("/")
    url = f"{base}/api/internal/commodities/batch"
    try:
        with httpx.Client(timeout=30.0, trust_env=False) as client:
            r = client.post(url, json=ids)
            r.raise_for_status()
            body = r.json()
        if not body.get("success") or not body.get("data"):
            return {}
        data = body["data"]
        if not isinstance(data, list):
            return {}
        return {str(c.get("commodityId")): c for c in data if c.get("commodityId")}
    except Exception:
        return {}
