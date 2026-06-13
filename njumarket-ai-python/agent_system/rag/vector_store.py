from __future__ import annotations

from agent_system.core.tools.milvus_tool import MilvusTool
from agent_system.rag.embedding import embed_texts


class LocalVectorStore:
    def __init__(self) -> None:
        self._milvus = MilvusTool()
        self.enabled = self._milvus.enabled

    def search_commodity_by_text(self, query: str, limit: int):
        if not self.enabled:
            return []
        vec = embed_texts([query])[0]
        rows = self._milvus.run(op="search", vector=vec, limit=limit)
        hits = []
        # pymilvus search result shape may vary; normalize to {id, score}
        for group in rows or []:
            for item in group:
                entity = getattr(item, "entity", None)
                iid = None
                if isinstance(entity, dict):
                    iid = entity.get("id")
                if iid is None and isinstance(item, dict):
                    iid = item.get("id") or item.get("entity", {}).get("id")
                score = getattr(item, "score", None)
                if score is None and isinstance(item, dict):
                    score = item.get("score")
                if iid is not None:
                    hits.append({"id": str(iid), "score": score})
        return hits


def get_vector_store():
    return LocalVectorStore()

