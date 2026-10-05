"""
픽봇 질문 분해 — LLM 1차 호출 + 지역 맵

사용자 질문을 "희망 지역"과 "그 밖의 모든 조건(방문 목적·분위기·메뉴·시설 등)"으로 나눈다.

1. LLM 1차 호출: 지역 표현을 사용자가 쓴 그대로 뽑고(오탈자만 고침) 포함/제외와 조건 문장을 나눈다.
   변환 추론이 없어 reasoning_effort=low 로 빠르게 돈다.
2. 지역 맵(region_map): 표현을 카페 주소에 실제로 있는 구·동 이름(허용 지역)으로 바꾼다.
3. 맵에 없는 표현만 LLM 에 실제 동·구를 묻고(최후 방어선), 표현과 답을 후보 파일에 모아 맵을 늘린다.
   지역까지 말했는데 지역을 무시하고 서울 전역에서 추천하는 것보다 몇 초 더 쓰는 편이 낫다고 봤다.

지역은 주소 필터에 쓰고, 나머지 조건은 리뷰 벡터 검색과 LLM 판정 호출에 쓴다.
"""

from __future__ import annotations
import logging
from dataclasses import dataclass, field

from config import Settings
from llm_client import call_llm
from region_map import CandidateRecorder, RegionMap, pick_allowed

logger = logging.getLogger(__name__)

REGION_MAP = RegionMap.load()

_recorders: dict[str, CandidateRecorder] = {}


def _recorder(settings: Settings) -> CandidateRecorder:
    path = settings.region_candidates_path
    if path not in _recorders:
        _recorders[path] = CandidateRecorder(path)
    return _recorders[path]


@dataclass
class ParsedQuery:
    regions: list[str] = field(default_factory=list)            # 포함할 지역 — 허용 목록에 있는 값만
    unmatched_regions: list[str] = field(default_factory=list)  # 말했지만 데이터에 없는 지역 — 사용자 표현 그대로, 필터에 쓰지 않음(로그용)
    exclude_regions: list[str] = field(default_factory=list)    # 제외할 지역 — 허용 목록에 있는 값만
    purpose: str = ""                                            # 지역을 뺀 나머지 조건


SYSTEM_PROMPT = """당신은 카페 검색 질문을 분해하는 도구입니다.
사용자 질문을 "지역 표현"과 "그 밖의 모든 조건"으로 나눠 JSON 객체 하나만 반환하세요. 설명 텍스트 절대 금지.

## 규칙
1. 사용자가 가고 싶은 지역 표현을 사용자가 쓴 그대로 regions 에 넣습니다.
   - 동네 이름·구 이름·역 이름·상권·거리 별칭("연트럴파크", "가로수길") 모두 지역 표현입니다.
   - 다른 지명으로 바꾸거나 넓히지 마세요. 단, 오탈자는 바른 표기로 고칩니다. ("영남동" → "연남동")
   - "근처", "쪽", "앞", "부근" 같은 말은 빼고 지명만 씁니다.
   - "A나 B", "A 또는 B"처럼 지역이 여러 개면 모두 넣습니다.
2. "A 말고", "A 빼고", "A 제외", "A는 싫고"처럼 피하고 싶은 지역 표현은 같은 방식으로 exclude_regions 에 넣습니다.
3. purpose 에는 지역 표현을 뺀 나머지 조건(방문 목적, 분위기, 메뉴, 시설, 주차, 좌석 등)을 자연스러운 한 문장으로 적습니다.
   지역 말고 다른 조건이 없으면 빈 문자열로 둡니다.
4. 지역을 말하지 않았으면 regions, exclude_regions 는 빈 배열입니다.
5. 모든 카페가 서울에 있으므로 "서울", "서울 전체", "서울 어디든", "아무 데나"는 지역 표현이 아닙니다.
6. 음식·음료·메뉴·인테리어 스타일을 꾸미는 지명("스페인 츄러스", "프랑스식 인테리어")은 지역이 아닙니다.
   지역 배열에 넣지 말고 purpose 에 그대로 남기세요.

## 형식
{"regions": [], "exclude_regions": [], "purpose": ""}

## 예시
질문: 홍대 근처에서 노트북 하기 좋은 조용한 카페
{"regions": ["홍대"], "exclude_regions": [], "purpose": "노트북 하기 좋은 조용한 카페"}

질문: 합정역 앞에 주차 가능한 카페 있어?
{"regions": ["합정"], "exclude_regions": [], "purpose": "주차 가능한 카페"}

질문: 강남이나 잠실 쪽 디저트 맛있는 곳
{"regions": ["강남", "잠실"], "exclude_regions": [], "purpose": "디저트가 맛있는 카페"}

질문: 성수 말고 사진 찍기 좋은 카페
{"regions": [], "exclude_regions": ["성수"], "purpose": "사진 찍기 좋은 카페"}

질문: 판교에 브런치 카페 추천해줘
{"regions": ["판교"], "exclude_regions": [], "purpose": "브런치 카페"}

질문: 건대 카페 추천해줘
{"regions": ["건대"], "exclude_regions": [], "purpose": ""}

질문: 영남동 조용한 카페
{"regions": ["연남동"], "exclude_regions": [], "purpose": "조용한 카페"}

질문: 서울 어디든 상관없으니 루프탑 카페
{"regions": [], "exclude_regions": [], "purpose": "루프탑이 있는 카페"}

질문: 스페인 츄러스 파는 카페
{"regions": [], "exclude_regions": [], "purpose": "스페인 츄러스를 파는 카페"}

질문: 창가 자리가 넓고 채광 좋은 카페
{"regions": [], "exclude_regions": [], "purpose": "창가 자리가 넓고 채광 좋은 카페"}"""


RESOLVE_SYSTEM_PROMPT = """당신은 사람들이 부르는 지역 표현을 실제 서울 행정구역으로 바꾸는 도구입니다.
JSON 객체 하나만 반환하세요. 설명 텍스트 절대 금지.

## 할 일
카페를 찾는 사용자가 말한 지역 표현들이 주어집니다. 표현마다 그곳이 있는 서울의 동 이름(areas)과 구 이름(gu)을 적습니다.
- 역 이름·상권·거리 별칭·줄임말도 그 장소가 있는 동·구로 바꿉니다. 여러 동이나 구에 걸치면 모두 적습니다.
- 동은 "연남동"처럼 법정동 이름으로, 구는 "마포구"처럼 적습니다.
- 서울 밖이면 areas, gu 모두 빈 배열입니다.
- 어디인지 확실하지 않으면 추측하지 말고 모두 빈 배열로 둡니다.
- 주어진 표현을 하나도 빠뜨리지 말고, 키는 주어진 표현 그대로 씁니다.

## 형식
{"results": {"<표현>": {"areas": ["<동>"], "gu": ["<구>"]}}}

## 예시
표현: ["망리단길", "판교"]
{"results": {"망리단길": {"areas": ["망원동"], "gu": ["마포구"]}, "판교": {"areas": [], "gu": []}}}"""


async def parse_query(query: str, allowed_regions: list[str], settings: Settings) -> ParsedQuery:
    """질문을 지역·조건으로 나눈다. LLM 1차 호출이 실패하면 질문 전체를 조건으로 보고 지역 필터 없이 진행한다."""
    try:
        raw = await call_llm(
            system_prompt=SYSTEM_PROMPT,
            user_message=f"질문: {query}",
            model=settings.llm_model,
            base_url=settings.llm_base_url,
            api_key=settings.llm_api_key,
            timeout=settings.llm_parse_timeout,
            reasoning_effort=settings.llm_reasoning_effort or None,
        )
    except Exception as e:
        logger.warning("질문 분해 LLM 호출 실패, 질문 전체를 조건으로 사용: %s", e)
        return ParsedQuery(purpose=query)
    if not isinstance(raw, dict):
        logger.warning("질문 분해 응답이 객체가 아님, 질문 전체를 조건으로 사용: %r", raw)
        return ParsedQuery(purpose=query)

    include = _dedupe(_str_list(raw.get("regions")))
    exclude = _dedupe(_str_list(raw.get("exclude_regions")))
    resolved = await resolve_regions(include + exclude, set(allowed_regions), query, settings)

    purpose = raw.get("purpose")
    return ParsedQuery(
        regions=_dedupe([r for e in include for r in resolved[e]]),
        unmatched_regions=[e for e in include if not resolved[e]],
        exclude_regions=_dedupe([r for e in exclude for r in resolved[e]]),
        purpose=purpose.strip() if isinstance(purpose, str) else "",
    )


async def resolve_regions(expressions: list[str], allowed: set[str], query: str,
                          settings: Settings) -> dict[str, list[str]]:
    """지역 표현 → 허용 지역 값 목록. 데이터에 없는 곳이면 빈 목록.

    맵에서 먼저 찾고, 맵에 없는 표현만 LLM 에 한 번에 묻는다. 물어본 표현은 후보 파일에 기록한다.
    LLM 호출이 실패하면 그 표현은 데이터에 없는 곳으로 보고 지역 필터에 쓰지 않는다.
    """
    resolved: dict[str, list[str]] = {}
    unknown: list[str] = []
    for e in dict.fromkeys(expressions):
        hit = REGION_MAP.resolve(e, allowed)
        if hit is None:
            unknown.append(e)
        else:
            resolved[e] = hit
    if not unknown:
        return resolved

    answers = await _ask_llm_regions(unknown, settings)
    recorder = _recorder(settings)
    for e in unknown:
        answer = answers.get(e) if answers is not None else None
        areas = answer["areas"] if answer else None
        gu = answer["gu"] if answer else None
        recorder.record(e, query, areas, gu)
        resolved[e] = pick_allowed(areas or [], gu or [], allowed)
        logger.info("지역 맵에 없는 표현 %r → LLM 답 areas=%s gu=%s → 필터 %s", e, areas, gu, resolved[e])
    return resolved


async def _ask_llm_regions(expressions: list[str], settings: Settings) -> dict[str, dict] | None:
    """맵에 없는 지역 표현의 실제 동·구를 LLM 에 묻는다. 실패하면 None.

    정확도가 중요해 추론량은 지정하지 않고(모델 기본값), 타임아웃도 1차 호출보다 길게 둔다.
    """
    try:
        raw = await call_llm(
            system_prompt=RESOLVE_SYSTEM_PROMPT,
            user_message=f"표현: {expressions}",
            model=settings.llm_model,
            base_url=settings.llm_base_url,
            api_key=settings.llm_api_key,
            timeout=settings.llm_region_timeout,
        )
    except Exception as e:
        logger.warning("지역 표현 LLM 확인 실패, 지역 필터 없이 진행: %s (%s)", expressions, e)
        return None
    results = raw.get("results") if isinstance(raw, dict) else None
    if not isinstance(results, dict):
        logger.warning("지역 표현 LLM 응답 형식 오류: %r", raw)
        return None
    answers = {}
    for e in expressions:
        item = results.get(e)
        if isinstance(item, dict):
            answers[e] = {"areas": _str_list(item.get("areas")), "gu": _str_list(item.get("gu"))}
    return answers


def _str_list(value) -> list[str]:
    if not isinstance(value, list):
        return []
    return [v.strip() for v in value if isinstance(v, str) and v.strip()]


def _dedupe(values: list[str]) -> list[str]:
    return list(dict.fromkeys(values))
