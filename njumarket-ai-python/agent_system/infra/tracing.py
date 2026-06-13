from __future__ import annotations

from contextlib import contextmanager
from time import perf_counter


@contextmanager
def traced(name: str):
    start = perf_counter()
    try:
        yield
    finally:
        _ = perf_counter() - start
        # 预留接入 OpenTelemetry / APM

