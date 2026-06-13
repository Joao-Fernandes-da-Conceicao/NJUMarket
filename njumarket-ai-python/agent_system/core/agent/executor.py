from __future__ import annotations

from typing import Any, AsyncIterator

from langchain_core.messages import HumanMessage, SystemMessage

from agent_system.config.settings import get_settings
from agent_system.core.agent.base_agent import AgentRequest, AgentResponse, BaseAgent
from agent_system.core.llm.client import get_llm_client
from agent_system.core.agent.shopping_graph import build_shopping_graph
from agent_system.core.tools.commodity_toolkit import CommodityToolkit
from agent_system.memory.chat_memory import build_invoke_messages
from agent_system.memory.persistence import count_user_assistant_messages
from agent_system.memory.summarizer import compress_chat_memory_if_needed


def _token_from_chat_chunk(chunk: Any) -> str:
    if chunk is None:
        return ""
    c = getattr(chunk, "content", None)
    if isinstance(c, str):
        return c
    if isinstance(c, list):
        parts: list[str] = []
        for block in c:
            if isinstance(block, dict) and block.get("type") == "text":
                parts.append(str(block.get("text") or ""))
        return "".join(parts)
    return str(c) if c is not None else ""


class Executor:
    """执行器：LangGraph 显式编排（检索 → 筛选 → 反思 → 回复），非购物意图直出。"""

    def __init__(self) -> None:
        self._agent = BaseAgent()
        self._commodity_toolkit = CommodityToolkit()
        self._graph = None
        self._last_stream_augment: str = ""
        self._last_stream_reply: str = ""

    def _get_graph(self):
        if self._graph is None:
            self._graph = build_shopping_graph(self._commodity_toolkit)
        return self._graph

    def execute(self, req: AgentRequest) -> AgentResponse:
        memory_id = req.conversation_id or "default"
        self._commodity_toolkit.begin_turn(memory_id)
        try:
            ua_count = count_user_assistant_messages(memory_id)
            compress_chat_memory_if_needed(memory_id, req.user_id, ua_count)

            system = self._commodity_toolkit.system_prompt_section()
            graph = self._get_graph()
            s_cfg = get_settings()
            invoke_msgs, aug = build_invoke_messages(
                user_id=req.user_id,
                conversation_id=memory_id,
                user_message=req.user_message,
                skill_system_prompt=system,
                chat_memory_buffer=s_cfg.chat_memory_buffer,
            )
            aug_text = aug.to_appendix()
            result = graph.invoke(
                {
                    "messages": invoke_msgs,
                    "user_message": req.user_message,
                    "retrieve_attempts": 0,
                    "filter_attempts": 0,
                },
                config={"recursion_limit": 40},
            )
            reply = (result.get("final_reply") or "").strip()
            if not reply:
                return self._agent.run(req, augment_text=aug_text)

            rec_ids = self._commodity_toolkit.get_recommended_ids(memory_id)
            rec_items = self._commodity_toolkit.get_recommended_items(memory_id)
            return AgentResponse(
                reply=reply,
                recommended_commodities=rec_items,
                recommended_commodity_ids=rec_ids,
                augment_description=aug_text,
            )
        finally:
            self._commodity_toolkit.end_turn(memory_id)

    def get_recommended_for_memory(self, conversation_id: str | None) -> tuple[list[str], list[dict]]:
        mid = conversation_id or "default"
        return (
            self._commodity_toolkit.get_recommended_ids(mid),
            self._commodity_toolkit.get_recommended_items(mid),
        )

    def take_last_stream_augment(self) -> str:
        return self._last_stream_augment

    def take_last_stream_reply(self) -> str:
        return self._last_stream_reply

    async def execute_stream(self, req: AgentRequest) -> AsyncIterator[str]:
        """LangGraph astream_events + on_chat_model_stream：与 llm.stream 节点对齐的原生 token 流。"""
        memory_id = req.conversation_id or "default"
        self._commodity_toolkit.begin_turn(memory_id)
        self._last_stream_reply = ""
        try:
            ua_count = count_user_assistant_messages(memory_id)
            compress_chat_memory_if_needed(memory_id, req.user_id, ua_count)

            system = self._commodity_toolkit.system_prompt_section()
            graph = self._get_graph()
            s_cfg = get_settings()
            invoke_msgs, aug = build_invoke_messages(
                user_id=req.user_id,
                conversation_id=memory_id,
                user_message=req.user_message,
                skill_system_prompt=system,
                chat_memory_buffer=s_cfg.chat_memory_buffer,
            )
            aug_text = aug.to_appendix()
            self._last_stream_augment = aug_text

            input_state: dict[str, Any] = {
                "messages": invoke_msgs,
                "user_message": req.user_message,
                "retrieve_attempts": 0,
                "filter_attempts": 0,
            }
            config: dict[str, Any] = {"recursion_limit": 40}

            streamed_pieces: list[str] = []
            chain_final: str = ""

            async for event in graph.astream_events(input_state, config=config, version="v2"):
                et = event.get("event")
                meta = event.get("metadata") or {}
                node = meta.get("langgraph_node", "")

                if et == "on_chat_model_stream" and node in ("respond_general", "respond_shopping"):
                    chunk = (event.get("data") or {}).get("chunk")
                    tok = _token_from_chat_chunk(chunk)
                    if tok:
                        streamed_pieces.append(tok)
                        yield tok

                if et == "on_chain_end" and node in ("respond_general", "respond_shopping"):
                    out = (event.get("data") or {}).get("output")
                    if isinstance(out, dict):
                        fr = (out.get("final_reply") or "").strip()
                        if fr:
                            chain_final = fr

            reply = "".join(streamed_pieces).strip() or chain_final
            if not reply:
                result = await graph.ainvoke(input_state, config=config)
                reply = (result.get("final_reply") or "").strip()

            if not reply:
                llm = get_llm_client()
                msgs: list[Any]
                if aug_text:
                    msgs = [
                        SystemMessage(
                            content="以下为本轮辅助上下文（Redis 画像、语义召回等，非用户原话），请结合用户问题作答：\n"
                            + aug_text
                        ),
                        HumanMessage(content=req.user_message),
                    ]
                else:
                    msgs = [HumanMessage(content=req.user_message)]
                buf: list[str] = []
                async for chunk in llm.astream(msgs):
                    tok = _token_from_chat_chunk(chunk)
                    if tok:
                        buf.append(tok)
                        yield tok
                reply = "".join(buf).strip()

            self._last_stream_reply = reply
        finally:
            self._commodity_toolkit.end_turn(memory_id)
