"""
用户画像向量召回：与 Java MilvusVectorService.searchUserProfileByText + bizId 过滤对齐。
"""
from __future__ import annotations

from typing import Any

from agent_system.config.settings import get_settings
from agent_system.rag.embedding import embed_texts


def _vector_field() -> str:
    return get_settings().milvus_vector_field or "embedding"

try:
    from pymilvus import MilvusClient
except Exception:  # pragma: no cover
    MilvusClient = None  # type: ignore


def _parse_hits(raw: Any) -> list[tuple[Any, dict[str, Any]]]:
    out: list[tuple[Any, dict[str, Any]]] = []
    for group in raw or []:
        for hit in group:
            entity: dict[str, Any] = {}
            score = getattr(hit, "score", None)
            if isinstance(hit, dict):
                score = hit.get("score", score)
                er = hit.get("entity")
                if isinstance(er, dict):
                    entity = er
            else:
                er = getattr(hit, "entity", None)
                if isinstance(er, dict):
                    entity = er
            out.append((score, entity))
    return out


def search_user_profile_vector_recall(
    user_id: str,
    query_text: str,
    top_k: int | None = None,
) -> str:
    """用语义向量在用户画像集合中检索，仅保留 bizId == user_id 的命中（与 Java 一致）。"""
    s = get_settings()
    if not s.milvus_enabled or MilvusClient is None:
        return ""
    tk = top_k if top_k is not None else s.user_profile_recall_top_k
    try:
        client = MilvusClient(
            uri=s.milvus_uri,
            token=s.milvus_token or None,
            db_name=s.milvus_db_name,
        )
        vec = embed_texts([query_text])[0]
        raw = client.search(
            collection_name=s.milvus_user_profile_collection,
            data=[vec],
            limit=max(tk * 12, tk),
            output_fields=["bizId", "content"],
        )
        lines: list[str] = []
        for score, entity in _parse_hits(raw):
            biz = entity.get("bizId")
            if biz is None or str(biz) != str(user_id):
                continue
            content = entity.get("content")
            if not content:
                continue
            lines.append(f"- [{score}] {content}")
            if len(lines) >= tk:
                break
        return "\n".join(lines).strip()
    except Exception:
        return ""


def search_conversation_vector_recall(
    conversation_id: str,
    query_text: str,
    top_k: int | None = None,
) -> str:
    """按 conversation_id 在会话向量集合做语义召回，返回可读片段。"""
    s = get_settings()
    if not conversation_id or not query_text:
        return ""
    if not s.milvus_enabled or MilvusClient is None:
        return ""
    tk = top_k if top_k is not None else s.milvus_memory_top_k
    try:
        client = MilvusClient(
            uri=s.milvus_uri,
            token=s.milvus_token or None,
            db_name=s.milvus_db_name,
        )
        vec = embed_texts([query_text])[0]
        raw = client.search(
            collection_name=s.milvus_conversation_collection,
            data=[vec],
            limit=max(tk * 2, tk),
            output_fields=["conversationId", "content"],
            filter=f'conversationId == "{conversation_id}"',
        )
        lines: list[str] = []
        for _, entity in _parse_hits(raw):
            content = str(entity.get("content") or "").strip()
            if not content:
                continue
            if content.startswith("role=user\ncontent=") and "\nrole=assistant\ncontent=" in content:
                text = content[len("role=user\ncontent=") :]
                split = text.find("\nrole=assistant\ncontent=")
                user_part = text[:split].strip()
                assistant_part = text[split + len("\nrole=assistant\ncontent=") :].strip()
                lines.append(f"[轮次] 用户: {user_part} | 助手: {assistant_part}")
            elif content.startswith("role=user\ncontent="):
                lines.append("[用户片段] " + content[len("role=user\ncontent=") :].strip())
            elif content.startswith("role=assistant\ncontent="):
                lines.append("[助手片段] " + content[len("role=assistant\ncontent=") :].strip())
            else:
                lines.append(content)
            if len(lines) >= tk:
                break
        return "\n".join(lines).strip()
    except Exception:
        return ""


def upsert_user_profile_vector_by_text(vector_id: str, user_id: str, chunk_text: str) -> None:
    """写入用户画像向量（与 Java upsertUserProfileVectorByText 对齐）。"""
    s = get_settings()
    if not s.milvus_enabled or MilvusClient is None:
        return
    try:
        client = MilvusClient(
            uri=s.milvus_uri,
            token=s.milvus_token or None,
            db_name=s.milvus_db_name,
        )
        vec = embed_texts([chunk_text])[0]
        vf = _vector_field()
        row = {
            "id": vector_id,
            "bizId": user_id,
            "content": chunk_text,
        }
        row[vf] = vec
        client.upsert(collection_name=s.milvus_user_profile_collection, data=[row])
    except Exception:
        return


def upsert_conversation_turn_vector(
    *,
    conversation_id: str,
    pair_id: str,
    user_text: str,
    assistant_text: str,
) -> None:
    """写入「一轮 user+assistant」向量，不抛出异常。"""
    s = get_settings()
    if not s.milvus_enabled or MilvusClient is None:
        return
    try:
        client = MilvusClient(
            uri=s.milvus_uri,
            token=s.milvus_token or None,
            db_name=s.milvus_db_name,
        )
        vector_content = f"role=user\ncontent={user_text or ''}\nrole=assistant\ncontent={assistant_text or ''}"
        vec = embed_texts([vector_content])[0]
        vf = _vector_field()
        row = {
            "id": pair_id,
            "conversationId": conversation_id,
            "content": vector_content,
        }
        row[vf] = vec
        client.upsert(collection_name=s.milvus_conversation_collection, data=[row])
    except Exception:
        return


def get_profile_summary_from_vector(user_id: str) -> dict[str, Any] | None:
    """供 /profile 接口：从向量库聚合可读摘要（多路召回）。"""
    s = get_settings()
    if not s.milvus_enabled:
        return None
    parts: list[str] = []
    for q in ("用户购物偏好与画像", "用户历史兴趣"):
        chunk = search_user_profile_vector_recall(user_id, q, top_k=5)
        if chunk:
            parts.append(chunk)
    text = "\n".join(parts).strip()
    if not text:
        return None
    return {
        "userId": user_id,
        "profileSummary": text,
        "updatedAt": None,
        "source": "milvus",
    }
