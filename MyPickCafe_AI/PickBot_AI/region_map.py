"""
픽봇 지역 표현 → 카페 주소 지역 변환

LLM 1차 호출은 사용자가 쓴 지역 표현("연트럴파크", "망원동", "잠실역 근처")을 그대로 뽑고,
허용 지역(카페 주소에 실제로 있는 구·동 이름)으로 바꾸는 일은 여기서 맵(region_aliases.json)으로 한다.
LLM 이 변환까지 추론하면 느리고(5초 초과가 잦음) 추론을 줄이면 엉뚱한 동네로 바꾸기도 했다
(실측: reasoning_effort=low 에서 "연트럴파크" → 성수동).

맵에 없는 표현만 LLM 에 실제 동·구를 묻고(query_parser), 그 표현과 답을 후보 파일에 모아 맵을 늘린다.
"""

from __future__ import annotations
import json
import logging
import re
import threading
from datetime import datetime
from pathlib import Path

logger = logging.getLogger(__name__)

_MAP_PATH = Path(__file__).parent / "region_aliases.json"

# 지명 뒤에 붙는 말. 공백을 뺀 뒤 끝에서부터 반복해서 뗀다. ("잠실역 근처" → "잠실역" → "잠실")
# "성수동1가", "을지로3가" 의 "N가"
_GA = re.compile(r"\d+가$")

_SUFFIXES = ("근처", "부근", "주변", "일대", "인근", "방면", "쪽", "앞", "역")


def compact(expression: str) -> str:
    return "".join(expression.split())


def normalize(expression: str) -> str:
    """공백과 뒤에 붙는 말을 뗀다. 지명이 통째로 지워지면 공백만 뺀 값을 쓴다."""
    key = compact(expression)
    changed = True
    while changed:
        changed = False
        for suffix in _SUFFIXES:
            if key.endswith(suffix) and len(key) > len(suffix):
                key = key[: -len(suffix)]
                changed = True
    return key


def pick_allowed(areas: list[str], gu: list[str], allowed: set[str]) -> list[str]:
    """동 중 데이터에 있는 것, 없으면 구 중 데이터에 있는 것. 둘 다 없으면 빈 목록.

    주소의 동 자리는 번지 앞 이름이라 "성수동1가" 같은 법정동도 "성수동"으로 맞춰 본다.
    """
    hits = []
    for a in areas:
        for name in (a, _GA.sub("", a)):
            if name in allowed and name not in hits:
                hits.append(name)
    return hits or [g for g in gu if g in allowed]


class RegionMap:
    def __init__(self, aliases: dict[str, dict]) -> None:
        self._aliases = aliases

    @classmethod
    def load(cls, path: Path = _MAP_PATH) -> "RegionMap":
        with open(path, encoding="utf-8") as f:
            return cls(json.load(f)["aliases"])

    def resolve(self, expression: str, allowed: set[str]) -> list[str] | None:
        """지역 표현을 허용 지역 값으로 바꾼다.

        - 찾으면 허용 지역 값 목록. 맵에 있지만 데이터에 카페가 없는 곳(서울 밖 등)이면 빈 목록.
        - 맵에서도 못 찾으면 None — 호출하는 쪽이 LLM 에 묻는다.
        """
        raw, key = compact(expression), normalize(expression)
        # 맵을 먼저 본다. "홍대입구"처럼 허용 지역 값이면서 더 넓은 범위(연남동 포함)를 가리키는 표현이 있다.
        for k in (raw, key):
            entry = self._aliases.get(k)
            if entry is not None:
                return pick_allowed(entry.get("areas", []), entry.get("gu", []), allowed)
        for k in (raw, key):
            if k in allowed:
                return [k]
        # 맵에 없어도 "성수" → 성수동, "강동" → 강동구 처럼 동·구를 붙이면 허용 지역인 경우
        for k in (key + "동", key + "구"):
            if k in allowed:
                return [k]
        # "인천 송도", "분당 정자동"처럼 지명 여러 개를 한 표현으로 받으면 단어마다 찾아 모두 찾을 때만 합친다
        parts = expression.split()
        if len(parts) > 1:
            found = [self.resolve(p, allowed) for p in parts]
            if all(f is not None for f in found):
                return list(dict.fromkeys(r for f in found for r in f))
        return None

    def is_outside(self, expression: str) -> bool:
        """맵에 서울 밖(outside)으로 적힌 지명인지. 띄어 쓴 지명은 단어 중 하나라도 서울 밖이면 서울 밖이다."""
        for k in (compact(expression), normalize(expression)):
            entry = self._aliases.get(k)
            if entry is not None:
                return bool(entry.get("outside"))
        parts = expression.split()
        return len(parts) > 1 and any(self.is_outside(p) for p in parts)


class CandidateRecorder:
    """맵에 없어 LLM 에 물어본 지역 표현을 모은다. 검토해서 region_aliases.json 에 옮긴다.

    표현별로 횟수·처음/마지막 시각·예시 질문·LLM 이 답한 동·구를 한 파일(JSON)에 누적한다.
    기록 실패는 추천을 막지 않도록 로그만 남긴다.
    """

    def __init__(self, path: str | Path) -> None:
        self._path = Path(path)
        self._lock = threading.Lock()

    def record(self, expression: str, query: str, areas: list[str] | None, gu: list[str] | None) -> None:
        key = normalize(expression)
        now = datetime.now().isoformat(timespec="seconds")
        try:
            with self._lock:
                data = self._read()
                item = data.get(key) or {"count": 0, "first_seen": now, "expressions": [], "examples": []}
                item["count"] += 1
                item["last_seen"] = now
                if expression not in item["expressions"]:
                    item["expressions"].append(expression)
                if query not in item["examples"] and len(item["examples"]) < 5:
                    item["examples"].append(query)
                # LLM 답이 없으면(호출 실패) 이전 답을 지우지 않는다
                if areas is not None or gu is not None:
                    item["llm"] = {"areas": areas or [], "gu": gu or []}
                data[key] = item
                self._path.parent.mkdir(parents=True, exist_ok=True)
                tmp = self._path.with_suffix(".tmp")
                tmp.write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")
                tmp.replace(self._path)
        except Exception as e:
            logger.warning("지역 매핑 후보 기록 실패 (%s): %s", expression, e)

    def _read(self) -> dict:
        if not self._path.exists():
            return {}
        try:
            return json.loads(self._path.read_text(encoding="utf-8"))
        except Exception as e:
            logger.warning("지역 매핑 후보 파일을 읽지 못해 새로 만든다: %s", e)
            return {}
