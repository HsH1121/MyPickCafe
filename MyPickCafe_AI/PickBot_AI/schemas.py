from pydantic import BaseModel


class PickBotRequest(BaseModel):
    query: str


class PickBotResult(BaseModel):
    cafeId:   int
    cafeName: str
    address:  str
    snippet:  str
    score:    float | None = None  # 조건 없이 순위만 매긴 결과는 유사도가 없다


class PickBotResponse(BaseModel):
    results: list[PickBotResult]
    notice:  str | None = None  # 예: "REGION_NOT_FOUND" — 말한 지역에 등록된 카페가 없음


class IndexOneRequest(BaseModel):
    reviewId:   int
    cafeId:     int
    cafeName:   str
    address:    str
    reviewText: str


class DeleteOneRequest(BaseModel):
    reviewId: int
