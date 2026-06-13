from __future__ import annotations

from collections import deque
from dataclasses import dataclass, field


@dataclass
class ShortTermMemory:
    """短期记忆（进程内窗口）。"""

    max_turns: int = 20
    turns: deque[str] = field(default_factory=deque)

    def append(self, text: str) -> None:
        self.turns.append(text)
        while len(self.turns) > self.max_turns:
            self.turns.popleft()

    def dump(self) -> list[str]:
        return list(self.turns)

