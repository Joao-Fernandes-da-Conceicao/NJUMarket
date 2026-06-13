from __future__ import annotations

from typing import Sequence

from langchain_openai import OpenAIEmbeddings

from agent_system.config.settings import get_settings


def embed_texts(texts: Sequence[str]) -> list[list[float]]:
    s = get_settings()
    emb = OpenAIEmbeddings(
        model=s.doubao_embedding_model,
        api_key=s.doubao_api_key,
        base_url=s.doubao_base_url,
    )
    return emb.embed_documents(list(texts))


__all__ = ["embed_texts"]

