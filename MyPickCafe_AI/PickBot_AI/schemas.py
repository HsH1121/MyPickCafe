from pydantic import BaseModel


class PickBotRequest(BaseModel):
    query: str


class PickBotResult(BaseModel):
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
