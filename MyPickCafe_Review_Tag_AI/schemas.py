from pydantic import BaseModel
from typing import Optional


class ReviewRequest(BaseModel):
    reviewId:   int
    reviewText: str


class ReviewAnalyzeResponse(BaseModel):
    sentiment: Optional[str] = None  # "GOOD" | "BAD" | null
    FACILITY:  list[str] = []
    MENU:      list[str] = []
    PURPOSE:   list[str] = []
    MOOD:      list[str] = []


class ChatbotRequest(BaseModel):
    query: str


class ChatbotResult(BaseModel):
    cafeId:   int
    cafeName: str
    address:  str
    snippet:  str
    score:    float
