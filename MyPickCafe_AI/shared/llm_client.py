"""
OpenAI 호환 /chat/completions 비동기 클라이언트
- response_format 으로 JSON 포맷 붕괴 방지
- stream: false 로 단일 응답 수신
- Ollama / Fireworks 등 OpenAI 호환 엔드포인트를 base_url 로 전환
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
    api_key: str | None = None,
    timeout: int,
    _attempt: int = 0,
) -> dict:
    """
    OpenAI 호환 Chat Completions API를 호출하고 파싱된 JSON dict를 반환합니다.
    실패 시 최대 3회까지 재시도합니다.

    Args:
        base_url: OpenAI 호환 베이스 URL (예: https://api.fireworks.ai/inference/v1)
        api_key:  Bearer 토큰. Ollama 등 인증이 없는 엔드포인트면 None.

    Raises:
        httpx.ConnectError       — 서비스에 연결 불가 (재시도 후에도 실패 시)
        httpx.TimeoutException   — 응답 시간 초과
        httpx.HTTPStatusError    — 4xx/5xx 반환
        ValueError               — 응답 본문이 유효한 JSON이 아님
    """
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user",   "content": user_message},
        ],
        "stream": False,
        "response_format": {"type": "json_object"},
        "temperature": 0.0,
        "top_p": 0.9,
        "max_tokens": 1000,
    }

    headers = {"Authorization": f"Bearer {api_key}"} if api_key else {}

    logger.debug("LLM request payload (attempt=%d): %s", _attempt + 1, json.dumps(payload, ensure_ascii=False))

    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(timeout)) as client:
            response = await client.post(
                f"{base_url}/chat/completions",
                json=payload,
                headers=headers,
            )
            response.raise_for_status()

        data = response.json()
        raw_content: str = data["choices"][0]["message"]["content"]
        logger.debug("LLM raw content: %s", raw_content)

        return json.loads(raw_content)

    except (httpx.HTTPStatusError, httpx.TransportError, ValueError, KeyError, IndexError) as exc:
        if _attempt < _MAX_ATTEMPTS - 1:
            logger.warning("LLM 호출 실패 (attempt=%d), 재시도: %s", _attempt + 1, exc)
            return await call_ollama(
                system_prompt=system_prompt,
                user_message=user_message,
                model=model,
                base_url=base_url,
                api_key=api_key,
                timeout=timeout,
                _attempt=_attempt + 1,
            )
        logger.error("LLM 최종 실패 (attempt=%d): %s", _attempt + 1, exc)
        raise