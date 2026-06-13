from __future__ import annotations

from agent_system.rag.retriever import retrieve_commodity_docs


def rag_context(query: str, top_k: int = 10) -> dict:
    hits = retrieve_commodity_docs(query, top_k)
    return {"query": query, "hits": hits}

