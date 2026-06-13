"""新架构入口：独立 FastAPI 应用，与 njumarket-service-ai AIAgentController 对齐。"""

from __future__ import annotations

import json
import uuid
from typing import Any

from fastapi import FastAPI, Header, HTTPException, Query, Request
from fastapi.responses import StreamingResponse

from agent_system.app.internal_routes import router as internal_router
from agent_system.app.result_dto import Result
from agent_system.infra.logging import setup_logging

setup_logging()
from agent_system.config.settings import get_settings
from agent_system.integrations.commodity_internal import fetch_commodities_batch
from agent_system.core.agent.base_agent import AgentRequest
from agent_system.core.agent.executor import Executor
from agent_system.memory.persistence import (
    append_message,
    create_or_get_conversation,
    count_user_assistant_messages,
    get_latest_message,
    get_messages,
    get_profile_summary,
    get_user_conversations,
    increment_message_count,
    update_conversation_window_state,
)
from agent_system.memory.profile_jobs import schedule_profile_update
from agent_system.memory.user_profile_vector import (
    get_profile_summary_from_vector,
    upsert_conversation_turn_vector,
)

app = FastAPI(title="agent_system", version="1.0.0")
app.include_router(internal_router)
executor = Executor()


def _resolve_user_id(
    x_user_id: str | None,
    user_id_query: str | None,
    body_user: str | None,
) -> str | None:
    return (x_user_id or user_id_query or body_user or "").strip() or None


@app.get("/health")
def health():
    return {"status": "ok", "service": "agent_system"}


@app.get("/actuator/health")
def actuator_health():
    return {"status": "UP"}


def _commodity_batch(ids: list[str]) -> dict[str, dict[str, Any]]:
    return fetch_commodities_batch(ids)


def _persist_after_reply(
    *,
    cid: str,
    user_id: str,
    user_message: str,
    reply: str,
    rec_ids: list[str],
) -> None:
    mid_u = append_message(cid, user_id, "user", user_message, None)
    mid_a = append_message(cid, user_id, "assistant", reply, rec_ids)
    upsert_conversation_turn_vector(
        conversation_id=cid,
        pair_id=f"pair_{mid_u}_{mid_a}",
        user_text=user_message,
        assistant_text=reply,
    )
    wc = count_user_assistant_messages(cid)
    update_conversation_window_state(cid, wc)
    increment_message_count(cid, 2) 
    schedule_profile_update(user_id, cid)


@app.post("/api/user/ai-agent/chat")
async def chat(request: Request):
    body_json: dict[str, Any] = {}
    ct = request.headers.get("content-type", "")
    if "application/json" in ct:
        try:
            body_json = await request.json()
            if not isinstance(body_json, dict):
                body_json = {}
        except Exception:
            body_json = {}

    q = request.query_params
    message = q.get("message") or body_json.get("message")
    cid = q.get("conversationId") or body_json.get("conversationId") or body_json.get("conversation_id")
    uid = _resolve_user_id(
        request.headers.get("X-User-Id"),
        q.get("userId"),
        body_json.get("userId") or body_json.get("user_id"),
    )

    if not message or not str(message).strip():
        raise HTTPException(status_code=400, detail="message 不能为空")
    if not uid:
        raise HTTPException(status_code=400, detail="需要 X-User-Id 或 userId")

    message = str(message).strip()
    cid = (str(cid).strip() if cid else "") or str(uuid.uuid4())
    title = message[:50] if message else "新对话"
    create_or_get_conversation(cid, uid, title)

    agent_req = AgentRequest(user_message=message, user_id=uid, conversation_id=cid)
    res = executor.execute(agent_req)
    _persist_after_reply(
        cid=cid,
        user_id=uid,
        user_message=message,
        reply=res.reply,
        rec_ids=res.recommended_commodity_ids,
    )

    return Result.ok_message(
        "AI Agent 回复成功",
        {
            "reply": res.reply,
            "recommendedCommodities": res.recommended_commodities,
            "conversationId": cid,
            "hasRecommendations": bool(res.recommended_commodities),
            "sessionAugmentation": res.augment_description or None,
        },
    ).model_dump(exclude_none=True)


@app.get("/api/user/ai-agent/chat-stream")
async def chat_stream(
    message: str = Query(..., description="用户消息"),
    conversationId: str | None = Query(None),
    userId: str | None = Query(None),
    x_user_id: str | None = Header(default=None, alias="X-User-Id"),
):
    uid = _resolve_user_id(x_user_id, userId, None)
    if not uid:
        raise HTTPException(status_code=400, detail="需要 X-User-Id 或 userId")
    if not message or not str(message).strip():
        raise HTTPException(status_code=400, detail="message 不能为空")

    message = str(message).strip()
    cid = (str(conversationId).strip() if conversationId else "") or str(uuid.uuid4())
    create_or_get_conversation(cid, uid, message[:50] if message else "新对话")

    agent_req = AgentRequest(user_message=message, user_id=uid, conversation_id=cid)

    async def event_gen():
        parts: list[str] = []
        try:
            async for chunk in executor.execute_stream(agent_req):
                parts.append(chunk)
                yield f"event: token\ndata: {json.dumps(chunk, ensure_ascii=False)}\n\n"
            reply = (executor.take_last_stream_reply() or "".join(parts)).strip()
            rec_ids, rec_items = executor.get_recommended_for_memory(cid)
            aug = executor.take_last_stream_augment()
            _persist_after_reply(
                cid=cid,
                user_id=uid,
                user_message=message,
                reply=reply,
                rec_ids=rec_ids,
            )
            payload = {
                "reply": reply,
                "conversationId": cid,
                "recommendedCommodities": rec_items,
                "hasRecommendations": bool(rec_items),
                "sessionAugmentation": aug or None,
            }
            yield f"event: complete\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"
        except Exception as e:
            err = json.dumps({"error": str(e).replace('"', "'")}, ensure_ascii=False)
            yield f"event: error\ndata: {err}\n\n"

    return StreamingResponse(event_gen(), media_type="text/event-stream")


@app.get("/api/user/ai-agent/chats")
def chats(
    limit: int = 50,
    userId: str | None = Query(None),
    x_user_id: str | None = Header(default=None, alias="X-User-Id"),
):
    uid = _resolve_user_id(x_user_id, userId, None)
    if not uid:
        raise HTTPException(status_code=400, detail="需要 X-User-Id 或 userId")
    convs = get_user_conversations(uid, limit)
    result = []
    for c in convs:
        item = {
            "conversationId": c.get("conversation_id"),
            "title": c.get("title"),
            "messageCount": c.get("message_count"),
            "status": c.get("status"),
            "createdAt": c.get("created_at").isoformat() if c.get("created_at") else None,
            "updatedAt": c.get("updated_at").isoformat() if c.get("updated_at") else None,
        }
        lm = get_latest_message(c.get("conversation_id"))
        if lm:
            preview = lm.get("content") or ""
            item["lastMessage"] = preview[:60] + ("…" if len(preview) > 60 else "")
            item["lastMessageRole"] = lm.get("role")
            item["lastMessageTime"] = lm.get("created_at").isoformat() if lm.get("created_at") else None
        result.append(item)
    return Result.ok_message("获取聊天列表成功", result).model_dump(exclude_none=True)


@app.get("/api/user/ai-agent/chats/{conversation_id}/messages")
def chat_messages(
    conversation_id: str,
    limit: int = 100,
    userId: str | None = Query(None),
    x_user_id: str | None = Header(default=None, alias="X-User-Id"),
):
    uid = _resolve_user_id(x_user_id, userId, None)
    if not uid:
        raise HTTPException(status_code=400, detail="需要 X-User-Id 或 userId")
    rows = get_messages(conversation_id, uid, limit)
    all_ids: set[str] = set()
    for m in rows:
        rec = m.get("recommended_commodity_ids")
        if not rec:
            continue
        try:
            all_ids.update(json.loads(rec))
        except Exception:
            pass
    cmap = _commodity_batch(list(all_ids))
    out = []
    for m in rows:
        ids: list[str] = []
        rec = m.get("recommended_commodity_ids")
        if rec:
            try:
                ids = json.loads(rec)
            except Exception:
                ids = []
        out.append(
            {
                "messageId": m.get("message_id"),
                "conversationId": m.get("conversation_id"),
                "role": m.get("role"),
                "content": m.get("content"),
                "createdAt": m.get("created_at").isoformat() if m.get("created_at") else None,
                "recommendedCommodityIds": ids,
                "recommendedCommodities": [cmap[i] for i in ids if i in cmap],
            }
        )
    return Result.ok_message("获取消息列表成功", out).model_dump(exclude_none=True)


@app.get("/api/user/ai-agent/profile")
def profile(
    userId: str | None = Query(None),
    x_user_id: str | None = Header(default=None, alias="X-User-Id"),
):
    uid = _resolve_user_id(x_user_id, userId, None)
    if not uid:
        raise HTTPException(status_code=400, detail="需要 X-User-Id 或 userId")
    p = get_profile_summary(uid)
    if p and (p.get("profileSummary") or "").strip():
        return Result.ok_message("获取画像成功", p).model_dump(exclude_none=True)
    pv = get_profile_summary_from_vector(uid)
    if not pv:
        return Result.ok_message("暂无画像，继续聊天后自动生成", None).model_dump(exclude_none=True)
    return Result.ok_message("获取画像成功", pv).model_dump(exclude_none=True)


def main():
    import uvicorn

    s = get_settings()
    uvicorn.run("agent_system.app.main:app", host=s.host, port=s.server_port, reload=True)


__all__ = ["app", "main"]


if __name__ == "__main__":
    main()
