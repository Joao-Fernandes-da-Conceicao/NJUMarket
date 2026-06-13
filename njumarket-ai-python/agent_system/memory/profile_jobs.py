"""异步用户画像更新：与 Java AIAgentService.updateUserProfileAsync 对齐（每 10 条消息触发）。"""
from __future__ import annotations

import threading
import time
from typing import Any

from langchain_core.messages import HumanMessage

from agent_system.core.llm.client import get_llm_client
from agent_system.memory.persistence import (
    count_messages,
    get_recent_messages_for_profile,
    save_profile_summary,
)
from agent_system.memory.user_profile_vector import upsert_user_profile_vector_by_text


PROFILE_UPDATE_INTERVAL = 10


def _build_profile_chunks(summary: str, recent_messages: list[dict[str, Any]]) -> list[str]:
    chunks: list[str] = []
    s = (summary or "").strip()
    if s:
        chunks.append("用户长期偏好摘要: " + s)
    dialog_lines: list[str] = []
    count = 0
    for msg in recent_messages:
        if count >= 12:
            break
        role = "用户" if (msg.get("role") or "").lower() == "user" else "助手"
        dialog_lines.append(f"{role}：{msg.get('content') or ''}")
        count += 1
    dialog_text = "\n".join(dialog_lines)
    chunk_size = 220
    for i in range(0, len(dialog_text), chunk_size):
        part = dialog_text[i : i + chunk_size].strip()
        if part:
            chunks.append("近期对话偏好片段: " + part)
    return chunks


def _run_profile_update(user_id: str, conversation_id: str) -> None:
    try:
        msg_count = count_messages(conversation_id)
        if msg_count <= 0 or msg_count % PROFILE_UPDATE_INTERVAL != 0:
            return
        recent = get_recent_messages_for_profile(conversation_id, user_id, 30)
        if not recent:
            return
        sb = []
        for msg in recent:
            role = "用户" if (msg.get("role") or "").lower() == "user" else "助手"
            sb.append(f"[{role}]: {msg.get('content') or ''}")
        prompt = (
            "根据以下用户与购物助手的对话，用一句话（不超过100字）归纳该用户的购物偏好，"
            "包括商品类型、价格区间、地区等信息：\n\n"
            + "\n".join(sb)
            + "\n偏好摘要："
        )
        llm = get_llm_client()
        out = llm.invoke([HumanMessage(content=prompt)])
        text = out.content if isinstance(out.content, str) else str(out.content)
        summary = (text or "").strip()
        save_profile_summary(user_id, summary)
        chunks = _build_profile_chunks(summary, recent)
        for idx, chunk in enumerate(chunks):
            vid = f"up_{user_id}_{int(time.time() * 1000)}_{idx}"
            upsert_user_profile_vector_by_text(vid, user_id, chunk)
    except Exception:
        return


def schedule_profile_update(user_id: str, conversation_id: str) -> None:
    t = threading.Thread(
        target=_run_profile_update,
        args=(user_id, conversation_id),
        daemon=True,
    )
    t.start()
