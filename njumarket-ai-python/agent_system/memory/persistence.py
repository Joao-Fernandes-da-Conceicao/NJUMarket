from __future__ import annotations

import json
import uuid
from datetime import datetime
from typing import Any

from sqlalchemy import text

from agent_system.integrations.cache import get_cache_client
from agent_system.integrations.database import SessionLocal


def create_or_get_conversation(conversation_id: str, user_id: str, title: str) -> dict[str, Any]:
    now = datetime.now()
    with SessionLocal() as session:
        row = session.execute(
            text(
                """
                SELECT conversation_id, user_id, title, message_count, status, created_at, updated_at
                FROM nju_market.ai_conversations
                WHERE conversation_id = :cid
                """
            ),
            {"cid": conversation_id},
        ).mappings().first()
        if row:
            return dict(row)
        session.execute(
            text(
                """
                INSERT INTO nju_market.ai_conversations
                (conversation_id, user_id, title, message_count, status, window_message_count, memory_summary, created_at, updated_at)
                VALUES (:cid, :uid, :title, 0, 'ACTIVE', 0, NULL, :now, :now)
                """
            ),
            {
                "cid": conversation_id,
                "uid": user_id,
                "title": (title or "新对话")[:200],
                "now": now,
            },
        )
        session.commit()
        return {
            "conversation_id": conversation_id,
            "user_id": user_id,
            "title": (title or "新对话")[:200],
            "message_count": 0,
            "status": "ACTIVE",
            "created_at": now,
            "updated_at": now,
        }


def increment_message_count(conversation_id: str, inc: int) -> None:
    if inc <= 0:
        return
    with SessionLocal() as session:
        session.execute(
            text(
                """
                UPDATE nju_market.ai_conversations
                SET message_count = COALESCE(message_count, 0) + :inc,
                    updated_at = :now
                WHERE conversation_id = :cid
                """
            ),
            {"cid": conversation_id, "inc": inc, "now": datetime.now()},
        )
        session.commit()


def append_message(
    conversation_id: str,
    user_id: str,
    role: str,
    content: str,
    recommended_commodity_ids: list[str] | None = None,
) -> str:
    message_id = str(uuid.uuid4())
    rec_json = json.dumps(recommended_commodity_ids or [], ensure_ascii=False) if role == "assistant" else None
    with SessionLocal() as session:
        session.execute(
            text(
                """
                INSERT INTO nju_market.ai_messages
                (message_id, conversation_id, user_id, role, content, recommended_commodity_ids, created_at)
                VALUES (:mid, :cid, :uid, :role, :content, :rec, :now)
                """
            ),
            {
                "mid": message_id,
                "cid": conversation_id,
                "uid": user_id,
                "role": role,
                "content": content,
                "rec": rec_json,
                "now": datetime.now(),
            },
        )
        session.commit()
    return message_id


def get_user_conversations(user_id: str, limit: int = 50) -> list[dict[str, Any]]:
    with SessionLocal() as session:
        rows = session.execute(
            text(
                """
                SELECT conversation_id, user_id, title, message_count, status, created_at, updated_at
                FROM nju_market.ai_conversations
                WHERE user_id = :uid AND status = 'ACTIVE'
                ORDER BY updated_at DESC
                LIMIT :lim
                """
            ),
            {"uid": user_id, "lim": min(max(limit, 1), 200)},
        ).mappings().all()
        return [dict(r) for r in rows]


def get_latest_message(conversation_id: str) -> dict[str, Any] | None:
    with SessionLocal() as session:
        row = session.execute(
            text(
                """
                SELECT message_id, role, content, created_at
                FROM nju_market.ai_messages
                WHERE conversation_id = :cid
                ORDER BY created_at DESC
                LIMIT 1
                """
            ),
            {"cid": conversation_id},
        ).mappings().first()
        return dict(row) if row else None


def get_conversation_memory_snapshot(conversation_id: str) -> dict[str, Any] | None:
    """与 Java AIConversationStorage.getConversationMemorySnapshot 对齐：memory_summary + window_message_count。"""
    if not conversation_id:
        return None
    with SessionLocal() as session:
        row = session.execute(
            text(
                """
                SELECT memory_summary, window_message_count
                FROM nju_market.ai_conversations
                WHERE conversation_id = :cid
                """
            ),
            {"cid": conversation_id},
        ).mappings().first()
        return dict(row) if row else None


def get_messages(conversation_id: str, user_id: str, limit: int = 100) -> list[dict[str, Any]]:
    """最近 limit 条消息，时间正序（与 Java getMessages 取尾部子列表一致）。"""
    cap = min(max(limit, 1), 500)
    with SessionLocal() as session:
        rows = session.execute(
            text(
                """
                SELECT message_id, conversation_id, user_id, role, content, recommended_commodity_ids, created_at
                FROM nju_market.ai_messages
                WHERE conversation_id = :cid AND user_id = :uid
                ORDER BY created_at DESC
                LIMIT :lim
                """
            ),
            {"cid": conversation_id, "uid": user_id, "lim": cap},
        ).mappings().all()
        rev = list(reversed(list(rows)))
        return [dict(r) for r in rev]


def count_messages(conversation_id: str) -> int:
    with SessionLocal() as session:
        row = session.execute(
            text(
                """
                SELECT COUNT(*) AS c
                FROM nju_market.ai_messages
                WHERE conversation_id = :cid
                """
            ),
            {"cid": conversation_id},
        ).mappings().first()
        return int(row["c"]) if row and row.get("c") is not None else 0


def count_user_assistant_messages(conversation_id: str) -> int:
    with SessionLocal() as session:
        row = session.execute(
            text(
                """
                SELECT COUNT(*) AS c
                FROM nju_market.ai_messages
                WHERE conversation_id = :cid AND LOWER(role) IN ('user', 'assistant')
                """
            ),
            {"cid": conversation_id},
        ).mappings().first()
        return int(row["c"]) if row and row.get("c") is not None else 0


def update_conversation_window_state(conversation_id: str, window_message_count: int) -> None:
    """更新 window_message_count，不覆盖 memory_summary（摘要由后续摘要任务写入）。"""
    with SessionLocal() as session:
        session.execute(
            text(
                """
                UPDATE nju_market.ai_conversations
                SET window_message_count = :wc,
                    updated_at = :now
                WHERE conversation_id = :cid
                """
            ),
            {
                "cid": conversation_id,
                "wc": max(0, int(window_message_count)),
                "now": datetime.now(),
            },
        )
        session.commit()


def update_conversation_memory_summary(conversation_id: str, memory_summary: str) -> None:
    with SessionLocal() as session:
        session.execute(
            text(
                """
                UPDATE nju_market.ai_conversations
                SET memory_summary = :ms,
                    updated_at = :now
                WHERE conversation_id = :cid
                """
            ),
            {"cid": conversation_id, "ms": memory_summary, "now": datetime.now()},
        )
        session.commit()


def save_profile_summary(user_id: str, summary: str) -> None:
    if not user_id:
        return
    r = get_cache_client()
    key = f"ai:profile:{user_id}"
    now = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
    r.hset(key, mapping={"userId": user_id, "profileSummary": summary or "", "updatedAt": now})
    r.expire(key, 60 * 60 * 24 * 365)


def get_recent_messages_for_profile(
    conversation_id: str, user_id: str, limit: int = 30
) -> list[dict[str, Any]]:
    """最近若干条，时间正序（与 Java getRecentMessages 用于画像一致）。"""
    return get_recent_messages_chronological(conversation_id, user_id, limit)


def get_recent_messages_chronological(
    conversation_id: str, user_id: str, limit: int
) -> list[dict[str, Any]]:
    """取最近 limit 条消息，按时间正序（与 Java JpaAIConversationStorage.get_messages 窗口语义一致）。"""
    cap = min(max(limit, 1), 500)
    with SessionLocal() as session:
        rows = session.execute(
            text(
                """
                SELECT message_id, conversation_id, user_id, role, content, recommended_commodity_ids, created_at
                FROM nju_market.ai_messages
                WHERE conversation_id = :cid AND user_id = :uid
                ORDER BY created_at DESC
                LIMIT :lim
                """
            ),
            {"cid": conversation_id, "uid": user_id, "lim": cap},
        ).mappings().all()
        rev = list(reversed(list(rows)))
        return [dict(r) for r in rev]


def get_profile_summary(user_id: str) -> dict[str, Any] | None:
    r = get_cache_client()
    key = f"ai:profile:{user_id}"
    m = r.hgetall(key)
    if not m:
        return None
    return {
        "userId": m.get("userId", user_id),
        "profileSummary": m.get("profileSummary", ""),
        "updatedAt": m.get("updatedAt"),
    }
