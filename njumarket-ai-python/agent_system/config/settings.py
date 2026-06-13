from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

# 固定到项目根目录（njumarket-ai-python/），不受启动时工作目录影响
_PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
_ENV_FILE = _PROJECT_ROOT / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(_ENV_FILE),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    host: str = Field(default="0.0.0.0")
    server_port: int = Field(default=8099)

    doubao_api_key: str = Field(default="")
    doubao_base_url: str = Field(default="https://ark.cn-beijing.volces.com/api/v3")
    doubao_chat_model: str = Field(default="doubao-seed-1-6-250615")
    doubao_embedding_model: str = Field(default="doubao-embedding-text-240715")

    commodity_base_url: str = Field(default="http://localhost:8092")

    spring_datasource_url: str = Field(
        default="postgresql+psycopg://postgres:postgres@localhost:5432/njumarket"
    )
    spring_datasource_username: str = Field(default="postgres")
    spring_datasource_password: str = Field(default="postgres")
    redis_host: str = Field(default="localhost")
    redis_port: int = Field(default=6379)
    redis_username: str = Field(default="default")
    redis_password: str = Field(default="")
    ai_redis_database: int = Field(default=3)

    milvus_enabled: bool = Field(default=False)
    milvus_uri: str = Field(default="http://localhost:19530")
    milvus_token: str = Field(default="")
    milvus_db_name: str = Field(default="default")
    milvus_commodity_collection: str = Field(default="commodity_vectors")
    milvus_user_profile_collection: str = Field(default="user_profile_vectors")
    milvus_conversation_collection: str = Field(default="conversation_memory_vectors")
    milvus_dimension: int = Field(default=1024)
    milvus_top_k: int = Field(default=10)
    milvus_vector_field: str = Field(
        default="embedding",
        description="与 Java MilvusVectorService VECTOR_FIELD 一致",
    )
    milvus_memory_top_k: int = Field(default=12)
    user_profile_recall_top_k: int = Field(default=3)
    chat_memory_buffer: int = Field(default=32)

    def sqlalchemy_database_url(self) -> str:
        raw = (self.spring_datasource_url or "").strip()
        if raw.startswith("postgresql+"):
            return raw
        if raw.startswith("jdbc:postgresql://"):
            parsed = urlparse(raw[len("jdbc:") :])
            host = parsed.hostname or "localhost"
            port = parsed.port or 5432
            db = (parsed.path or "/njumarket").lstrip("/") or "njumarket"
            query = parse_qs(parsed.query or "")
            schema = query.get("currentSchema", [""])[0]
            user = self.spring_datasource_username
            pwd = self.spring_datasource_password
            suffix = f"?options=-csearch_path%3D{schema}" if schema else ""
            return f"postgresql+psycopg://{user}:{pwd}@{host}:{port}/{db}{suffix}"
        return raw or "postgresql+psycopg://postgres:postgres@localhost:5432/njumarket"


@lru_cache
def get_settings() -> Settings:
    import logging
    s = Settings()
    logging.getLogger(__name__).info(
        "Settings loaded: env_file=%s, doubao_api_key=%s, server_port=%s",
        _ENV_FILE,
        ("***" + s.doubao_api_key[-4:]) if len(s.doubao_api_key) > 4 else ("<EMPTY>" if not s.doubao_api_key else "****"),
        s.server_port,
    )
    return s


__all__ = ["Settings", "get_settings"]

