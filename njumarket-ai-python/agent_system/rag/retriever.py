from __future__ import annotations

from agent_system.rag.vector_store import get_vector_store


def retrieve_commodity_docs(query: str, limit: int = 10):
    store = get_vector_store()
    if not store.enabled:
        return []
    return store.search_commodity_by_text(query, limit)

