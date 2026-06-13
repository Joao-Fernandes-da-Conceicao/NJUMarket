from __future__ import annotations

from typing import Any

from agent_system.core.tools.base_tool import BaseTool


class CodeTool(BaseTool):
    name = "code-tool"

    def run(self, **kwargs: Any) -> Any:
        # 预留给代码分析/执行类能力；当前项目未启用。
        return {"enabled": False, "reason": "not configured"}

