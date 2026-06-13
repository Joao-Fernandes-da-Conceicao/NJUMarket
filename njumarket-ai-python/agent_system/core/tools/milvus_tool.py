from __future__ import annotations

from typing import Any

from agent_system.config.settings import get_settings
from agent_system.core.tools.base_tool import BaseTool

try:
    from pymilvus import MilvusClient
except Exception:  # pragma: no cover
    MilvusClient = None  # type: ignore


class MilvusTool(BaseTool):
    name = "milvus-tool"

    def __init__(self) -> None:
        s = get_settings()
        self._enabled = bool(s.milvus_enabled and MilvusClient is not None)
        self._collection = s.milvus_commodity_collection
        self._client = (
            MilvusClient(uri=s.milvus_uri, token=s.milvus_token or None, db_name=s.milvus_db_name)
            if self._enabled
            else None
        )

    @property
    def enabled(self) -> bool:
        return self._enabled

    def run(self, **kwargs: Any) -> Any:
        if not self._enabled or self._client is None:
            return []
        op = kwargs.get("op", "search")
        if op != "search":
            raise ValueError("milvus-tool only supports op=search")
        vector = kwargs.get("vector")
        limit = int(kwargs.get("limit", 10))
        if not isinstance(vector, list) or not vector:
            raise ValueError("vector is required for Milvus search")
        return self._client.search(
            collection_name=self._collection,
            data=[vector],
            limit=limit,
            output_fields=["id"],
        )

