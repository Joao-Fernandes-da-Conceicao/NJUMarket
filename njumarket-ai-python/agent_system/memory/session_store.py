from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime


@dataclass
class SessionMessage:
    conversation_id: str
    user_id: str
    role: str
    content: str
    created_at: datetime


_messages: list[SessionMessage] = []


def get_session_messages(conversation_id: str, user_id: str, limit: int = 100):
    rows = [
        m
        for m in _messages
        if m.conversation_id == conversation_id and m.user_id == user_id
    ]
    return rows[-limit:]


def append_session_message(conversation_id: str, user_id: str, role: str, content: str) -> None:
    _messages.append(
        SessionMessage(
            conversation_id=conversation_id,
            user_id=user_id,
            role=role,
            content=content,
            created_at=datetime.now(),
        )
    )

