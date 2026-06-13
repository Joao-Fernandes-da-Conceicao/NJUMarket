from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from agent_system.config.settings import get_settings

s = get_settings()
engine = create_engine(s.sqlalchemy_database_url(), future=True, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)

__all__ = ["engine", "SessionLocal"]

