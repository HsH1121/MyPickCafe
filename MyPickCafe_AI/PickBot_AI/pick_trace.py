"""
픽봇 단계별 추적 로그 — 개발 환경 전용

PICKBOT_TRACE=true 일 때만 추천 요청 하나마다 단계별 걸린 시간과 결과를 한 덩어리로 남긴다.
기본값은 꺼짐이고 배포 compose 에는 넣지 않는다. 꺼져 있으면 기록 호출은 아무 일도 하지 않는다.

출력: PickBot_AI/logs/pickbot_trace.log 와 콘솔(루트 로거).
"""

from __future__ import annotations
import logging
import time
from pathlib import Path

logger = logging.getLogger("pickbot_trace")
logger.setLevel(logging.INFO)

LOG_PATH = Path(__file__).parent / "logs" / "pickbot_trace.log"
_file_handler: logging.Handler | None = None


def _ensure_file_handler() -> None:
    """처음 기록할 때 파일 핸들러를 붙인다. 꺼진 환경에서는 로그 파일을 만들지 않는다."""
    global _file_handler
    if _file_handler is None or Path(_file_handler.baseFilename) != LOG_PATH:
        if _file_handler is not None:
            logger.removeHandler(_file_handler)
            _file_handler.close()
        LOG_PATH.parent.mkdir(parents=True, exist_ok=True)
        _file_handler = logging.FileHandler(LOG_PATH, encoding="utf-8")
        _file_handler.setFormatter(logging.Formatter("%(asctime)s %(message)s"))
        logger.addHandler(_file_handler)


def format_attempts(attempts: list[dict]) -> str:
    """call_llm 의 시도 기록 → "시도1 5.0s ReadTimeout, 시도2 1.2s 성공" (한 번에 성공하면 빈 문자열)."""
    if len(attempts) <= 1 and all(a.get("error") is None for a in attempts):
        return ""
    return ", ".join(f"시도{i} {a['sec']:.1f}s {a.get('error') or '성공'}" for i, a in enumerate(attempts, 1))


class PickTrace:
    def __init__(self, query: str, enabled: bool) -> None:
        self.enabled = enabled
        self.query = query
        self._start = time.perf_counter()
        self._lines: list[str] = []

    @staticmethod
    def now() -> float:
        return time.perf_counter()

    def step(self, label: str, started: float | None, detail: str = "") -> None:
        """단계 한 줄. started 가 None 이면 실행하지 않은 단계(시간 칸에 '-')."""
        if not self.enabled:
            return
        sec = f"{time.perf_counter() - started:5.2f}s" if started is not None else "    - "
        self._lines.append(f" {label}  {sec}  {detail}".rstrip())

    def note(self, detail: str) -> None:
        """바로 위 단계의 세부 내용 한 줄."""
        if self.enabled:
            self._lines.append(f"      {detail}")

    def finish(self, outcome: str) -> None:
        if not self.enabled:
            return
        _ensure_file_handler()
        total = time.perf_counter() - self._start
        logger.info("[픽봇 추적] %r  총 %.2fs → %s\n%s\n%s",
                    self.query, total, outcome, "\n".join(self._lines), "-" * 70)


NO_TRACE = PickTrace("", enabled=False)
