from __future__ import annotations

from typing import Any


def parse_text_output(raw: Any) -> str:
    if raw is None:
        return ""
    if isinstance(raw, str):
        return raw
    return str(raw)

