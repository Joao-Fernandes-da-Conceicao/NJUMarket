from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from langchain_core.messages import HumanMessage

from agent_system.core.llm.client import get_llm_client


@dataclass
class AgentRequest:
    user_message: str
    user_id: str
    conversation_id: str | None = None


@dataclass
class AgentResponse:
    reply: str
    recommended_commodities: list[dict[str, Any]]
    recommended_commodity_ids: list[str]
    augment_description: str = ""


class BaseAgent:
    """基础 Agent：独立 LLM 调用，不依赖旧 app 层。"""

    def run(self, req: AgentRequest, augment_text: str = "") -> AgentResponse:
        llm = get_llm_client()
        aug = (augment_text or "").strip()
        if aug:
            from langchain_core.messages import SystemMessage

            out = llm.invoke(
                [
                    SystemMessage(
                        content="以下为本轮辅助上下文（Redis 画像、语义召回等，非用户原话），请结合用户问题作答：\n"
                        + aug
                    ),
                    HumanMessage(content=req.user_message),
                ]
            )
        else:
            out = llm.invoke([HumanMessage(content=req.user_message)])
        text = out.content if isinstance(out.content, str) else str(out.content)
        return AgentResponse(
            reply=text,
            recommended_commodities=[],
            recommended_commodity_ids=[],
            augment_description=aug,
        )

