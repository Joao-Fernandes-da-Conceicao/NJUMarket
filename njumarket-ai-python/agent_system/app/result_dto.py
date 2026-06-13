from typing import Any

from pydantic import BaseModel


class Result(BaseModel):
    success: bool = True
    errorMsg: str | None = None
    data: Any = None
    total: int | None = None

    @staticmethod
    def ok_message(message: str, data: Any) -> "Result":
        return Result(success=True, errorMsg=message, data=data, total=None)

    @staticmethod
    def fail(msg: str) -> "Result":
        return Result(success=False, errorMsg=msg, data=None, total=None)
