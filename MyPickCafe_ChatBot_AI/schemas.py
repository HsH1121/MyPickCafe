from pydantic import BaseModel


class ChatbotRequest(BaseModel):
    query: str


class ChatbotResult(BaseModel):
    cafeId:   int
    cafeName: str
    address:  str
    snippet:  str
    score:    float


class IndexOneRequest(BaseModel):
    reviewId:   int
    cafeId:     int
    cafeName:   str
    address:    str
    reviewText: str


class DeleteOneRequest(BaseModel):
    reviewId: int
