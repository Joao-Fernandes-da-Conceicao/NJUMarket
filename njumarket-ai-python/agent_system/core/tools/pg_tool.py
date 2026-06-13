from __future__ import annotations

from typing import Any

from sqlalchemy import text

from agent_system.core.tools.base_tool import BaseTool
from agent_system.integrations.database import SessionLocal


class PGTool(BaseTool):
    name = "pg-tool"

    def run(self, **kwargs: Any) -> Any:
        sql = kwargs.get("sql")
        params = kwargs.get("params", {})
        if not sql:
            raise ValueError("sql is required")
        with SessionLocal() as session:
            rows = session.execute(text(sql), params).mappings().all()
            return [dict(r) for r in rows]

