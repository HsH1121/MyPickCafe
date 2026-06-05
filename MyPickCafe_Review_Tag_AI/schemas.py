from pydantic import BaseModel
from typing import Optional


class ReviewRequest(BaseModel):
    reviewId:   int
    reviewText: str


class ReviewAnalyzeResponse(BaseModel):
    sentiment: Optional[str] = None
    FACILITY:  list[str] = []
    MENU:      list[str] = []
    PURPOSE:   list[str] = []
    MOOD:      list[str] = []
