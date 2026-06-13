"""
从 PostgreSQL 恢复对话窗口，并拼接 System 增广（历史摘要 + Milvus 用户画像召回）。
使用 langchain_core BaseChatMessageHistory（PostgresChatMessageHistory）加载窗口消息，
与 RunnableWithMessageHistory / LangGraph MessagesState 生态一致。
与 Java AIAgentService.initChatMemory / prepareSessionAugmentation 语义对齐。
"""
from __future__ import annotations

from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage

from agent_system.memory.persistence import get_conversation_memory_snapshot
from agent_system.memory.postgres_chat_history import PostgresChatMessageHistory
from agent_system.memory.session_augmentation import SessionAugmentation, prepare_session_augmentation


def build_invoke_messages(
    *,
    user_id: str,
    conversation_id: str,
    user_message: str,
    skill_system_prompt: str,
    chat_memory_buffer: int,
) -> tuple[list[BaseMessage], SessionAugmentation]:
    """构造本轮 graph.invoke 的 messages：system + 历史 user/ai + 当前 user；并返回本轮会话增广对象。"""
    aug = prepare_session_augmentation(
        user_id=user_id,
        conversation_id=conversation_id,
        user_message=user_message,
    )

    snap = get_conversation_memory_snapshot(conversation_id)
    summary_body: str | None = None
    window_count = 0
    if snap:
        raw_sum = snap.get("memory_summary")
        if raw_sum and str(raw_sum).strip():
            summary_body = str(raw_sum).strip()
        try:
            window_count = int(snap.get("window_message_count") or 0)
        except (TypeError, ValueError):
            window_count = 0

    load_limit = min(window_count, chat_memory_buffer) if window_count > 0 else chat_memory_buffer
    history = PostgresChatMessageHistory(
        conversation_id=conversation_id,
        user_id=user_id,
        max_messages=load_limit,
    )

    system_parts: list[str] = [skill_system_prompt]
    if summary_body:
        system_parts.append("【历史摘要】" + summary_body)
    appendix = aug.to_appendix()
    if appendix:
        system_parts.append(appendix)

    messages: list[BaseMessage] = [SystemMessage(content="\n\n".join(system_parts))]
    messages.extend(history.messages)
    messages.append(HumanMessage(content=user_message))
    return messages, aug
