"""与 njumarket-service-ai InternalController 对齐的内部 API。"""
from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Body

from agent_system.app.result_dto import Result
from agent_system.services import commodity_enrichment
from agent_system.services.milvus_vector_internal import (
    parse_float_list,
    parse_top_k,
    search_commodity,
    search_user_profile,
    upsert_commodity,
    upsert_user_profile,
)

router = APIRouter(prefix="/api/internal", tags=["AI-internal"])


def _get_str(body: dict[str, Any], key: str) -> str | None:
    v = body.get(key)
    if v is None:
        return None
    return str(v).strip() or None


@router.post("/commodity-enrich")
def enrich_commodity_for_search(body: dict[str, Any] = Body(...)) -> dict[str, Any]:
    if not isinstance(body, dict):
        return Result.fail("请求体无效").model_dump(exclude_none=True)
    title = _get_str(body, "title")
    if not title:
        return Result.fail("缺少 title").model_dump(exclude_none=True)
    try:
        enriched = commodity_enrichment.enrich_for_search(
            title=title,
            description=_get_str(body, "description"),
            category=_get_str(body, "category"),
            condition_level=_get_str(body, "conditionLevel"),
            location=_get_str(body, "location"),
            address_snapshot_full=_get_str(body, "addressSnapshotFull"),
        )
        if enriched:
            return Result.ok_message(
                "丰度文本生成成功",
                {"enrichedKeywordPayload": enriched},
            ).model_dump(exclude_none=True)
        return Result.ok_message("未生成丰度文本（可回退使用原标题与描述）", None).model_dump(
            exclude_none=True
        )
    except Exception as e:
        return Result.fail(f"丰度增强失败: {e}").model_dump(exclude_none=True)


@router.post("/vector/commodity/upsert")
def upsert_commodity_vector(body: dict[str, Any] = Body(...)) -> dict[str, Any]:
    try:
        row_id = _get_str(body, "id")
        if not row_id:
            return Result.fail("id 不能为空").model_dump(exclude_none=True)
        biz_id = _get_str(body, "bizId")
        content = body.get("content")
        content_s = None if content is None else str(content)
        emb_raw = body.get("embedding")
        if emb_raw is not None:
            emb = parse_float_list(emb_raw)
            upsert_commodity(row_id=row_id, biz_id=biz_id, content=content_s, embedding=emb)
        else:
            upsert_commodity(row_id=row_id, biz_id=biz_id, content=content_s, embedding=None)
        return Result.ok_message("商品向量写入成功", None).model_dump(exclude_none=True)
    except Exception as e:
        return Result.fail(f"商品向量写入失败: {e}").model_dump(exclude_none=True)


@router.post("/vector/user-profile/upsert")
def upsert_user_profile_vector(body: dict[str, Any] = Body(...)) -> dict[str, Any]:
    try:
        row_id = _get_str(body, "id")
        if not row_id:
            return Result.fail("id 不能为空").model_dump(exclude_none=True)
        biz_id = _get_str(body, "bizId")
        content = body.get("content")
        content_s = None if content is None else str(content)
        emb_raw = body.get("embedding")
        if emb_raw is not None:
            emb = parse_float_list(emb_raw)
            upsert_user_profile(row_id=row_id, biz_id=biz_id, content=content_s, embedding=emb)
        else:
            upsert_user_profile(row_id=row_id, biz_id=biz_id, content=content_s, embedding=None)
        return Result.ok_message("用户画像向量写入成功", None).model_dump(exclude_none=True)
    except Exception as e:
        return Result.fail(f"用户画像向量写入失败: {e}").model_dump(exclude_none=True)


@router.post("/vector/commodity/search")
def search_commodity_vector(body: dict[str, Any] = Body(...)) -> dict[str, Any]:
    try:
        top_k = parse_top_k(body.get("topK"))
        if body.get("embedding") is not None:
            emb = parse_float_list(body.get("embedding"))
            hits = search_commodity(embedding=emb, query_text=None, top_k=top_k)
        else:
            qt = _get_str(body, "queryText")
            if not qt:
                return Result.fail("queryText 与 embedding 至少填一项").model_dump(exclude_none=True)
            hits = search_commodity(embedding=None, query_text=qt, top_k=top_k)
        return Result.ok_message("商品向量检索成功", hits).model_dump(exclude_none=True)
    except Exception as e:
        return Result.fail(f"商品向量检索失败: {e}").model_dump(exclude_none=True)


@router.post("/vector/user-profile/search")
def search_user_profile_vector(body: dict[str, Any] = Body(...)) -> dict[str, Any]:
    try:
        top_k = parse_top_k(body.get("topK"))
        if body.get("embedding") is not None:
            emb = parse_float_list(body.get("embedding"))
            hits = search_user_profile(embedding=emb, query_text=None, top_k=top_k)
        else:
            qt = _get_str(body, "queryText")
            if not qt:
                return Result.fail("queryText 与 embedding 至少填一项").model_dump(exclude_none=True)
            hits = search_user_profile(embedding=None, query_text=qt, top_k=top_k)
        return Result.ok_message("用户画像向量检索成功", hits).model_dump(exclude_none=True)
    except Exception as e:
        return Result.fail(f"用户画像向量检索失败: {e}").model_dump(exclude_none=True)
