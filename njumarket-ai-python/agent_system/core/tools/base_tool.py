from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any


class BaseTool(ABC):
    name: str = "base-tool"

    @abstractmethod
    def run(self, **kwargs: Any) -> Any:
        raise NotImplementedError

