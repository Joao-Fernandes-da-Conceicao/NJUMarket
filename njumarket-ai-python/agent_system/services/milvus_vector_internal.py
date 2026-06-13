"""Milvus 向量读写：与 Java MilvusVectorService 对齐（供 /api/internal/vector/*）。"""
from __future__ import annotations

from typing import Any

from agent_system.config.settings import get_settings
from agent_system.rag.embedding import embed_texts

try:
    from pymilvus import MilvusClient
except Exception:  # pragma: no cover
    MilvusClient = None  # type: ignore


def _normalize_embedding(embedding: list[float]) -> list[float]:
    s = get_settings()
    target = s.milvus_dimension
    if len(embedding) == target:
        return embedding
    if len(embedding) > target:
        return list(embedding[:target])
    return list(embedding) + [0.0] * (target - len(embedding))


def _client() -> Any | None:
    s = get_settings()
    if not s.milvus_enabled or MilvusClient is None:
        return None
    return MilvusClient(uri=s.milvus_uri, token=s.milvus_token or None, db_name=s.milvus_db_name)


def _vector_field_name() -> str:
    return get_settings().milvus_vector_field or "embedding"


def _hits_to_java_shape(raw: Any) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for group in raw or []:
        for hit in group:
            score = getattr(hit, "score", None)
            entity: dict[str, Any] = {}
            hid: Any = None
            if isinstance(hit, dict):
                score = hit.get("score", score)
                hid = hit.get("id")
                er = hit.get("entity")
                if isinstance(er, dict):
                    entity = er
            else:
                er = getattr(hit, "entity", None)
                if isinstance(er, dict):
                    entity = er
                hid = getattr(hit, "id", None)
            if hid is None and entity.get("id") is not None:
                hid = entity.get("id")
            out.append({"id": hid, "score": score, "entity": entity})
    return out


def upsert_commodity(
    *,
    row_id: str,
    biz_id: str | None,
    content: str | None,
    embedding: list[float] | None,
) -> None:
    s = get_settings()
    c = _client()
    if c is None:
        raise RuntimeError("Milvus 未启用")
    vf = _vector_field_name()
    vec = _normalize_embedding(embedding if embedding is not None else embed_texts([content or ""])[0])
    row: dict[str, Any] = {
        "id": row_id,
        "bizId": (biz_id or row_id or "").strip() or row_id,
        "content": content or "",
    }
    row[vf] = vec
    c.upsert(collection_name=s.milvus_commodity_collection, data=[row])


def upsert_user_profile(
    *,
    row_id: str,
    biz_id: str | None,
    content: str | None,
    embedding: list[float] | None,
) -> None:
    s = get_settings()
    c = _client()
    if c is None:
        raise RuntimeError("Milvus 未启用")
    vf = _vector_field_name()
    vec = _normalize_embedding(embedding if embedding is not None else embed_texts([content or ""])[0])
    row: dict[str, Any] = {
        "id": row_id,
        "bizId": (biz_id or row_id or "").strip() or row_id,
        "content": content or "",
    }
    row[vf] = vec
    c.upsert(collection_name=s.milvus_user_profile_collection, data=[row])


def search_commodity(
    *,
    embedding: list[float] | None,
    query_text: str | None,
    top_k: int | None,
) -> list[dict[str, Any]]:
    s = get_settings()
    c = _client()
    if c is None:
        raise RuntimeError("Milvus 未启用")
    k = top_k if top_k is not None and top_k > 0 else s.milvus_top_k
    if embedding is not None:
        vec = _normalize_embedding(embedding)
    else:
        qt = (query_text or "").strip()
        if not qt:
            return []
        vec = embed_texts([qt])[0]
    raw = c.search(
        collection_name=s.milvus_commodity_collection,
        data=[vec],
        limit=k,
        output_fields=["id", "bizId", "content"],
    )
    return _hits_to_java_shape(raw)


def search_user_profile(
    *,
    embedding: list[float] | None,
    query_text: str | None,
    top_k: int | None,
) -> list[dict[str, Any]]:
    s = get_settings()
    c = _client()
    if c is None:
        raise RuntimeError("Milvus 未启用")
    k = top_k if top_k is not None and top_k > 0 else s.milvus_top_k
    if embedding is not None:
        vec = _normalize_embedding(embedding)
    else:
        qt = (query_text or "").strip()
        if not qt:
            return []
        vec = embed_texts([qt])[0]
    raw = c.search(
        collection_name=s.milvus_user_profile_collection,
        data=[vec],
        limit=k,
        output_fields=["id", "bizId", "content"],
    )
    return _hits_to_java_shape(raw)


def parse_float_list(raw: Any) -> list[float]:
    if not isinstance(raw, list):
        raise ValueError("embedding 不能为空且必须为数组")
    out: list[float] = []
    for item in raw:
        if not isinstance(item, (int, float)):
            raise ValueError("embedding 数组元素必须为数值")
        out.append(float(item))
    return out


def parse_top_k(raw: Any) -> int | None:
    if raw is None:
        return None
    if isinstance(raw, (int, float)):
        return int(raw)
    try:
        return int(str(raw).strip())
    except Exception as e:
        raise ValueError("topK 必须为整数") from e
