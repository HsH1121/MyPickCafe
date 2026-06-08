"""
Ollama /api/chat 비동기 클라이언트
- "format": "json" 옵션으로 JSON 포맷 붕괴 방지
- stream: false 로 단일 응답 수신
"""

from __future__ import annotations
import json
import logging

import httpx

logger = logging.getLogger(__name__)


_MAX_ATTEMPTS = 3


async def call_ollama(
    *,
    system_prompt: str,
    user_message: str,
    model: str,
    base_url: str,
    timeout: int,
    _attempt: int = 0,
) -> dict:
    """
    Ollama Chat API를 호출하고 파싱된 JSON dict를 반환합니다.
    실패 시 최대 3회까지 재시도합니다 (Dummy 흐름의 재시도 정책 적용).

    Raises:
        httpx.ConnectError       — Ollama 서비스에 연결 불가 (재시도 후에도 실패 시)
        httpx.TimeoutException   — 응답 시간 초과
        httpx.HTTPStatusError    — Ollama가 4xx/5xx 반환
        ValueError               — 응답 본문이 유효한 JSON이 아님
    """
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user",   "content": user_message},
        ],
        "format": "json",
        "stream": False,
        "options": {
            "temperature": 0.0,
            "top_p": 0.9,
            "num_predict": 1000,
        },
    }

    logger.debug("Ollama request payload (attempt=%d): %s", _attempt + 1, json.dumps(payload, ensure_ascii=False))

    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(timeout)) as client:
            response = await client.post(f"{base_url}/api/chat", json=payload)
            response.raise_for_status()

        data = response.json()
        raw_content: str = data["message"]["content"]
        logger.debug("Ollama raw content: %s", raw_content)

        return json.loads(raw_content)

    except (httpx.HTTPStatusError, httpx.TransportError, ValueError) as exc:
        if _attempt < _MAX_ATTEMPTS - 1:
            logger.warning("Ollama 호출 실패 (attempt=%d), 재시도: %s", _attempt + 1, exc)
            return await call_ollama(
                system_prompt=system_prompt,
                user_message=user_message,
                model=model,
                base_url=base_url,
                timeout=timeout,
                _attempt=_attempt + 1,
            )
        logger.error("Ollama 최종 실패 (attempt=%d): %s", _attempt + 1, exc)
        raise
