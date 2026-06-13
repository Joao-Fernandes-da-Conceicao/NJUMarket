from __future__ import annotations

from typing import Any

from langchain_openai import ChatOpenAI

from agent_system.config.settings import get_settings


def get_llm_client(**overrides: Any):
    s = get_settings()
    kwargs: dict[str, Any] = {
        "model": s.doubao_chat_model,
        "api_key": s.doubao_api_key,
        "base_url": s.doubao_base_url,
        "temperature": 0,
        "max_tokens": 2000,
        "timeout": 120,
    }
    kwargs.update(overrides)
    return ChatOpenAI(**kwargs)

