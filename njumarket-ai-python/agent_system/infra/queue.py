from __future__ import annotations

from queue import Queue


class InMemoryQueue:
    def __init__(self) -> None:
        self._q: Queue = Queue()

    def put(self, item):
        self._q.put(item)

    def get(self):
        return self._q.get()

