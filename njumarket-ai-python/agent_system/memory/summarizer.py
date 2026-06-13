"""
对话摘要压缩：与 Java AIAgentService.summarizeAndCompress 对齐。

当会话窗口消息数超过 SUMMARY_TRIGGER 时，用 LLM 将旧消息压缩为摘要写入 DB，
保留最近 MAX_MEMORY_MESSAGES 条消息的 window_message_count。
"""
from __future__ import annotations

import threading
from typing import Any

from langchain_core.messages import HumanMessage

from agent_system.core.llm.client import get_llm_client
from agent_system.memory.persistence import (
    get_recent_messages_chronological,
    update_conversation_memory_summary,
    update_conversation_window_state,
)

SUMMARY_TRIGGER = 24
MAX_MEMORY_MESSAGES = 15


def _do_compress(conversation_id: str, user_id: str, total_ua_count: int) -> None:
    """实际执行摘要压缩（在后台线程中调用）。"""
    try:
        all_msgs = get_recent_messages_chronological(conversation_id, user_id, total_ua_count)
        if len(all_msgs) < SUMMARY_TRIGGER:
            return

        old_msgs = all_msgs[: len(all_msgs) - MAX_MEMORY_MESSAGES]
        if not old_msgs:
            return

        lines: list[str] = []
        for msg in old_msgs:
            role = (msg.get("role") or "").lower()
            content = msg.get("content") or ""
            if role == "user":
                lines.append(f"[用户]: {content}")
            elif role == "assistant":
                lines.append(f"[助手]: {content}")

        if not lines:
            return

        prompt = (
            "请用简洁的语言（不超过200字）总结以下对话的主要内容和用户需求偏好：\n\n"
            + "\n".join(lines)
            + "\n一段话总结："
        )
        llm = get_llm_client()
        out = llm.invoke([HumanMessage(content=prompt)])
        summary = (out.content if isinstance(out.content, str) else str(out.content)).strip()
        if not summary:
            return

        update_conversation_memory_summary(conversation_id, summary)
        update_conversation_window_state(conversation_id, MAX_MEMORY_MESSAGES)
    except Exception:
        return


def compress_chat_memory_if_needed(
    conversation_id: str,
    user_id: str,
    current_ua_count: int,
    *,
    blocking: bool = False,
) -> None:
    """
    检查窗口消息数是否超过阈值，若是则异步触发摘要压缩。

    与 Java AIAgentService.summarizeAndCompress 语义对齐：
    - SUMMARY_TRIGGER = 24（user+assistant 消息数）
    - MAX_MEMORY_MESSAGES = 15（压缩后保留的最近条数）

    :param blocking: 为 True 时在调用线程内同步执行（测试用），默认异步。
    """
    if not conversation_id or current_ua_count < SUMMARY_TRIGGER:
        return
    if blocking:
        _do_compress(conversation_id, user_id, current_ua_count)
    else:
        t = threading.Thread(
            target=_do_compress,
            args=(conversation_id, user_id, current_ua_count),
            daemon=True,
        )
        t.start()
