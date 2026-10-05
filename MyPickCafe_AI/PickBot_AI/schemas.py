from pydantic import BaseModel, Field

# 픽봇 질문 최대 글자 수 — 프론트엔드(cafes/list.mustache 의 QUERY_MAX_LENGTH)와 반드시 같은 값.
# 글자 수는 양쪽 모두 유니코드 코드 포인트 기준(Python len() = JS Array.from(s).length)이며,
# 공백·특수문자·이모지도 각각 1자로 센다. 앞뒤 공백을 잘라내지 않은 받은 그대로의 길이다.
QUERY_MAX_LENGTH = 30


class PickBotRequest(BaseModel):
    query: str = Field(max_length=QUERY_MAX_LENGTH)


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
