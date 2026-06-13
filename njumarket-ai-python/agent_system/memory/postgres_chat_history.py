"""
PostgreSQL 会话消息历史：langchain_core.chat_history.BaseChatMessageHistory 实现。

与 RunnableWithMessageHistory / LangGraph 侧「按 session 拉消息」的惯用法一致；
当前应用层仍在 main 中显式 append_message，本类在 invoke 路径上主要承担 **读取窗口**。
写入接口供后续接入原生 agent 收尾或工具链复用。
"""
from __future__ import annotations

from collections.abc import Sequence

from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage

from agent_system.memory.persistence import append_message, get_recent_messages_chronological


def _content_to_str(content: str | list[str | dict]) -> str:
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts: list[str] = []
        for block in content:
            if isinstance(block, dict) and block.get("type") == "text":
                parts.append(str(block.get("text") or ""))
            elif isinstance(block, str):
                parts.append(block)
        return "".join(parts)
    return str(content)


def _row_to_message(row: dict) -> BaseMessage | None:
    role = (row.get("role") or "").lower()
    content = row.get("content") or ""
    if role == "user":
        return HumanMessage(content=content)
    if role == "assistant":
        return AIMessage(content=content)
    return None


class PostgresChatMessageHistory(BaseChatMessageHistory):
    """从 nju_market.ai_messages 读取最近窗口；可选写入 user/assistant 行。"""

    def __init__(self, *, conversation_id: str, user_id: str, max_messages: int) -> None:
        self._conversation_id = conversation_id
        self._user_id = user_id
        self._max_messages = max(1, min(int(max_messages), 500))

    @property
    def messages(self) -> list[BaseMessage]:
        rows = get_recent_messages_chronological(
            self._conversation_id, self._user_id, self._max_messages
        )
        out: list[BaseMessage] = []
        for row in rows:
            m = _row_to_message(row)
            if m is not None:
                out.append(m)
        return out

    def add_messages(self, messages: Sequence[BaseMessage]) -> None:
        for message in messages:
            if isinstance(message, HumanMessage):
                append_message(
                    self._conversation_id,
                    self._user_id,
                    "user",
                    _content_to_str(message.content),
                    None,
                )
            elif isinstance(message, AIMessage):
                append_message(
                    self._conversation_id,
                    self._user_id,
                    "assistant",
                    _content_to_str(message.content),
                    None,
                )

    def clear(self) -> None:
        raise NotImplementedError(
            "会话历史由产品侧管理；如需关闭会话请更新 ai_conversations.status。"
        )
