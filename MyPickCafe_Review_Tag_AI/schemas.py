from pydantic import BaseModel
from typing import Optional


class ReviewRequest(BaseModel):
    reviewId:   int
    reviewText: str


class ReviewAnalyzeResponse(BaseModel):
    sentiment:    Optional[str] = None  # "GOOD" | "BAD" | null
    facilityTags: list[str] = []
    menuTags:     list[str] = []
    purposeTags:  list[str] = []
    moodTags:     list[str] = []
