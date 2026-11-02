"""
내부 호출 전용 인증 — Spring Boot 만 이 AI 서버를 부를 수 있게 한다.

이 서버의 엔드포인트(/pickbot/reindex·index-one·delete-one, /review/analyze 등)에는
사용자 인증이 없다. 사용자 인증·인가는 Spring Security 가 맡고, 이 서버는 Spring 이 대신
호출하는 내부 서비스라는 전제다. 포트를 외부에 열지 않는 것(docker-compose)이 1차 방어이고,
이 토큰은 같은 네트워크 안의 다른 컨테이너나 설정 실수로 포트가 열렸을 때를 위한 2차 방어다.

AI_INTERNAL_TOKEN 환경변수가
  - 비어 있으면: 검사하지 않는다(로컬 개발에서 기존처럼 바로 호출 가능).
  - 있으면:      /health 를 뺀 모든 요청에 X-Internal-Token 헤더가 같아야 한다. 다르면 401.
"""

from __future__ import annotations

import hmac
import logging
import os

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse

HEADER_NAME = "X-Internal-Token"

# 컨테이너 HEALTHCHECK(127.0.0.1)가 토큰 없이 부르는 경로
_OPEN_PATHS = frozenset({"/health"})

logger = logging.getLogger(__name__)


def internal_token_from_env() -> str:
    return os.getenv("AI_INTERNAL_TOKEN", "").strip()


def docs_enabled_from_env() -> bool:
    """/docs·/redoc·/openapi.json 노출 여부. 기본은 켬(로컬 개발), compose 는 false 로 끈다."""
    return os.getenv("AI_DOCS_ENABLED", "true").strip().lower() not in {"false", "0", "no", "off"}


class InternalTokenMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, token: str):
        super().__init__(app)
        self._token = token.encode("utf-8")

    async def dispatch(self, request: Request, call_next):
        if request.url.path in _OPEN_PATHS:
            return await call_next(request)
        supplied = request.headers.get(HEADER_NAME, "").encode("utf-8")
        # 길이·내용 비교 시간이 값에 따라 달라지지 않게 compare_digest 를 쓴다
        if not hmac.compare_digest(supplied, self._token):
            # 토큰 값은 남기지 않는다
            logger.warning("내부 토큰 불일치로 거부: %s %s from %s",
                           request.method, request.url.path,
                           request.client.host if request.client else "-")
            return JSONResponse({"detail": "unauthorized"}, status_code=401)
        return await call_next(request)


def install_internal_auth(app, token: str | None = None) -> bool:
    """토큰이 설정돼 있으면 미들웨어를 붙이고 True. 비어 있으면 아무것도 하지 않고 False."""
    token = internal_token_from_env() if token is None else token.strip()
    if not token:
        logger.warning("AI_INTERNAL_TOKEN 이 비어 있어 내부 호출 인증을 하지 않습니다. "
                       "배포 환경에서는 반드시 설정하세요.")
        return False
    app.add_middleware(InternalTokenMiddleware, token=token)
    return True
