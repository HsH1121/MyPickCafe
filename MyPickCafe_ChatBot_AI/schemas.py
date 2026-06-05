from pydantic import BaseModel


class ChatbotRequest(BaseModel):
    query: str


class ChatbotResult(BaseModel):
    cafeId:   int
    cafeName: str
    address:  str
    snippet:  str
    score:    float
