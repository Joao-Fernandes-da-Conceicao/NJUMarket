import redis

from agent_system.config.settings import get_settings


def get_cache_client() -> redis.Redis:
    s = get_settings()
    return redis.Redis(
        host=s.redis_host,
        port=s.redis_port,
        username=s.redis_username,
        password=s.redis_password,
        db=s.ai_redis_database,
        decode_responses=True,
    )


__all__ = ["get_cache_client"]

