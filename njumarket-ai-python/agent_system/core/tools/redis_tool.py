from __future__ import annotations

from typing import Any

from agent_system.core.tools.base_tool import BaseTool
from agent_system.integrations.cache import get_cache_client


class RedisTool(BaseTool):
    name = "redis-tool"

    def run(self, **kwargs: Any) -> Any:
        op = kwargs.get("op")
        key = kwargs.get("key")
        if not op or not key:
            raise ValueError("op and key are required")
        r = get_cache_client()
        if op == "get":
            return r.get(key)
        if op == "set":
            return r.set(key, kwargs.get("value", ""))
        if op == "hgetall":
            return r.hgetall(key)
        if op == "hset":
            mapping = kwargs.get("mapping", {})
            return r.hset(key, mapping=mapping)
        raise ValueError(f"unsupported redis op: {op}")

