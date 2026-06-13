from __future__ import annotations

from typing import Any

import httpx

from agent_system.config.settings import get_settings
from agent_system.core.tools.base_tool import BaseTool


class SearchTool(BaseTool):
    name = "search-tool"

    def run(self, **kwargs: Any) -> Any:
        query = kwargs.get("query")
        if not query:
            raise ValueError("query is required")
        location = kwargs.get("location")
        limit = kwargs.get("limit", 20)

        params: dict[str, Any] = {
            "keyword": query,
            "page": 1,
            "size": limit,
            "sortBy": "relevance",
        }
        if location:
            params["location"] = location
        url = f"{get_settings().commodity_base_url.rstrip('/')}/api/public/commodity/search"
        with httpx.Client(timeout=30.0, trust_env=False) as client:
            r = client.get(url, params=params)
            r.raise_for_status()
            return r.json()

