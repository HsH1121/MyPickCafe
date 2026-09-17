"""
픽봇 질문 분해 — LLM 1차 호출

사용자 질문을 "희망 지역"과 "그 밖의 모든 조건(방문 목적·분위기·메뉴·시설 등)"으로 나눈다.
지역은 카페 주소에 실제로 있는 구·동 이름으로 바꿔 받아 주소 필터에 쓰고,
나머지 조건은 리뷰 벡터 검색과 LLM 2차 호출에 쓴다.
"""

from __future__ import annotations
import logging
from dataclasses import dataclass, field

from config import Settings
from llm_client import call_llm

logger = logging.getLogger(__name__)


@dataclass
class ParsedQuery:
    regions: list[str] = field(default_factory=list)            # 포함할 지역 — 허용 목록에 있는 값만
    unmatched_regions: list[str] = field(default_factory=list)  # 말했지만 데이터에 없는 지역 — 사용자 표현 그대로
    exclude_regions: list[str] = field(default_factory=list)    # 제외할 지역 — 허용 목록에 있는 값만
    purpose: str = ""                                            # 지역을 뺀 나머지 조건

    @property
    def wants_region(self) -> bool:
        """사용자가 가고 싶은 지역을 말했는지 (데이터에 있든 없든)."""
        return bool(self.regions or self.unmatched_regions)


# 예시의 지역 값은 현재 카페 주소 데이터에 있는 구·동 이름 기준이다.
# 주소 데이터가 바뀌어 예시 값이 허용 목록에서 빠지더라도, 응답은 _validate 에서 허용 목록으로 다시 거른다.
_SYSTEM_PROMPT_TEMPLATE = """당신은 카페 검색 질문을 분해하는 도구입니다.
사용자 질문을 "지역 조건"과 "그 밖의 모든 조건"으로 나눠 JSON 객체 하나만 반환하세요. 설명 텍스트 절대 금지.

## 허용 지역 목록
카페 주소에 실제로 쓰이는 구·동 이름입니다. regions 와 exclude_regions 에는 이 목록의 값만 그대로 쓰세요.
__ALLOWED_REGIONS__

## 규칙
1. 사용자가 가고 싶은 지역을 허용 지역 목록의 값으로 바꿔 regions 에 넣습니다.
   - 목록에 그 동네를 가리키는 동 이름이 있으면 동 이름을, 없으면 그 동네가 속한 구 이름을 씁니다.
   - 통칭·역 이름·상권 이름("홍대", "건대", "강남역" 등)도 해당하는 목록 값으로 바꿉니다.
   - "A나 B", "A 또는 B"처럼 지역이 여러 개면 모두 넣습니다.
2. 지역을 말했지만 목록의 어떤 값에도 해당하지 않으면, 사용자가 쓴 표현 그대로 unmatched_regions 에 넣습니다.
3. "A 말고", "A 빼고", "A 제외", "A 아닌 곳"처럼 피하고 싶은 지역은 1번과 같은 방식으로 바꿔 exclude_regions 에 넣습니다.
4. purpose 에는 지역 표현을 뺀 나머지 조건(방문 목적, 분위기, 메뉴, 시설, 주차, 좌석 등)을 자연스러운 한 문장으로 적습니다.
   지역 말고 다른 조건이 없으면 빈 문자열로 둡니다.
5. 지역을 말하지 않았으면 regions, unmatched_regions, exclude_regions 는 모두 빈 배열입니다.

## 형식
{"regions": [], "unmatched_regions": [], "exclude_regions": [], "purpose": ""}

## 예시
질문: 홍대 근처에서 노트북 하기 좋은 조용한 카페
{"regions": ["홍대입구", "연남동"], "unmatched_regions": [], "exclude_regions": [], "purpose": "노트북 하기 좋은 조용한 카페"}

질문: 합정에 주차 가능한 카페 있어?
{"regions": ["마포구"], "unmatched_regions": [], "exclude_regions": [], "purpose": "주차 가능한 카페"}

질문: 강남이나 잠실 쪽 디저트 맛있는 곳
{"regions": ["강남구", "잠실동"], "unmatched_regions": [], "exclude_regions": [], "purpose": "디저트가 맛있는 카페"}

질문: 성수 말고 사진 찍기 좋은 카페
{"regions": [], "unmatched_regions": [], "exclude_regions": ["성수동"], "purpose": "사진 찍기 좋은 카페"}

질문: 판교에 브런치 카페 추천해줘
{"regions": [], "unmatched_regions": ["판교"], "exclude_regions": [], "purpose": "브런치 카페"}

질문: 건대 카페 추천해줘
{"regions": ["건대입구"], "unmatched_regions": [], "exclude_regions": [], "purpose": ""}

질문: 창가 자리가 넓고 채광 좋은 카페
{"regions": [], "unmatched_regions": [], "exclude_regions": [], "purpose": "창가 자리가 넓고 채광 좋은 카페"}"""


def build_system_prompt(allowed_regions: list[str]) -> str:
    return _SYSTEM_PROMPT_TEMPLATE.replace("__ALLOWED_REGIONS__", ", ".join(allowed_regions))


async def parse_query(query: str, allowed_regions: list[str], settings: Settings) -> ParsedQuery:
    """질문을 지역·조건으로 나눈다. LLM 호출이 실패하면 질문 전체를 조건으로 보고 지역 필터 없이 진행한다."""
    try:
        raw = await call_llm(
            system_prompt=build_system_prompt(allowed_regions),
            user_message=f"질문: {query}",
            model=settings.llm_model,
            base_url=settings.llm_base_url,
            api_key=settings.llm_api_key,
            timeout=settings.llm_timeout,
        )
    except Exception as e:
        logger.warning("질문 분해 LLM 호출 실패, 질문 전체를 조건으로 사용: %s", e)
        return ParsedQuery(purpose=query)
    if not isinstance(raw, dict):
        logger.warning("질문 분해 응답이 객체가 아님, 질문 전체를 조건으로 사용: %r", raw)
        return ParsedQuery(purpose=query)
    return _validate(raw, set(allowed_regions))


def _validate(raw: dict, allowed: set[str]) -> ParsedQuery:
    """LLM 이 허용 목록 밖의 지역을 regions 에 넣었으면 unmatched_regions 로 옮기고, 제외 지역은 목록 값만 남긴다."""
    regions: list[str] = []
    unmatched = _str_list(raw.get("unmatched_regions"))
    for r in _str_list(raw.get("regions")):
        (regions if r in allowed else unmatched).append(r)
    exclude = [r for r in _str_list(raw.get("exclude_regions")) if r in allowed]
    purpose = raw.get("purpose")
    return ParsedQuery(
        regions=_dedupe(regions),
        unmatched_regions=_dedupe(unmatched),
        exclude_regions=_dedupe(exclude),
        purpose=purpose.strip() if isinstance(purpose, str) else "",
    )


def _str_list(value) -> list[str]:
    if not isinstance(value, list):
        return []
    return [v.strip() for v in value if isinstance(v, str) and v.strip()]


def _dedupe(values: list[str]) -> list[str]:
    return list(dict.fromkeys(values))
