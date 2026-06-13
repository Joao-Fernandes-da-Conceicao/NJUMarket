from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

_profiles: dict[str, "Profile"] = {}


@dataclass
class Profile:
    user_id: str
    profile_summary: str
    updated_at: datetime


def get_profile(user_id: str):
    return _profiles.get(user_id)


def save_profile(user_id: str, summary: str) -> None:
    _profiles[user_id] = Profile(
        user_id=user_id,
        profile_summary=summary,
        updated_at=datetime.now(),
    )

