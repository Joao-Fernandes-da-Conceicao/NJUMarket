from __future__ import annotations

from typing import Any

from agent_system.core.tools.base_tool import BaseTool
from agent_system.memory.session_store import get_session_messages


class DBTool(BaseTool):
    name = "db-tool"

    def run(self, **kwargs: Any) -> Any:
        action = kwargs.get("action")
        if action == "get_messages":
            return get_session_messages(kwargs["conversation_id"], kwargs["user_id"], kwargs.get("limit", 100))
        if action == "get_user_conversations":
            # 当前无持久层接入，返回空列表占位。
            return []
        raise ValueError(f"unsupported action: {action}")

