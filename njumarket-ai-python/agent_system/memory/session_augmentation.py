from __future__ import annotations

from dataclasses import dataclass

from agent_system.memory.persistence import get_profile_summary
from agent_system.memory.user_profile_vector import (
    search_conversation_vector_recall,
    search_user_profile_vector_recall,
)


@dataclass
class SessionAugmentation:
    redis_profile_summary: str = ""
    conversation_semantic_recall: str = ""
    user_profile_semantic_recall: str = ""

    def is_empty(self) -> bool:
        return not (
            (self.redis_profile_summary or "").strip()
            or (self.conversation_semantic_recall or "").strip()
            or (self.user_profile_semantic_recall or "").strip()
        )

    def to_appendix(self) -> str:
        parts: list[str] = []
        if (self.redis_profile_summary or "").strip():
            parts.append(
                "=== 用户画像（Redis 摘要，辅助）===\n"
                + self.redis_profile_summary.strip()
                + "\n请结合以上偏好提供个性化建议。"
            )
        if (self.conversation_semantic_recall or "").strip():
            parts.append(
                "=== 语义召回 · 与本问题相关的历史对话片段（辅助，非完整聊天记录）===\n"
                + self.conversation_semantic_recall.strip()
            )
        if (self.user_profile_semantic_recall or "").strip():
            parts.append(
                "=== 语义召回 · 与本问题相关的用户画像片段（辅助）===\n"
                + self.user_profile_semantic_recall.strip()
            )
        return "\n\n".join(parts).strip()


def prepare_session_augmentation(
    *,
    user_id: str,
    conversation_id: str,
    user_message: str,
) -> SessionAugmentation:
    redis_summary = ""
    p = get_profile_summary(user_id)
    if p and p.get("profileSummary"):
        redis_summary = str(p.get("profileSummary") or "").strip()
    conv_recall = search_conversation_vector_recall(conversation_id, user_message)
    profile_recall = search_user_profile_vector_recall(user_id, user_message)
    return SessionAugmentation(
        redis_profile_summary=redis_summary,
        conversation_semantic_recall=conv_recall,
        user_profile_semantic_recall=profile_recall,
    )

