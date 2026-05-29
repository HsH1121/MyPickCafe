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


async def call_ollama(
    *,
    system_prompt: str,
    user_message: str,
    model: str,
    base_url: str,
    timeout: int,
) -> dict:
    """
    Ollama Chat API를 호출하고 파싱된 JSON dict를 반환합니다.

    Raises:
        httpx.ConnectError       — Ollama 서비스에 연결 불가
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
        "format": "json",   # JSON 포맷 강제 — 포맷 붕괴 방지 핵심 옵션
        "stream": False,
        "options": {
            "temperature": 0.1,   # 결정론적 출력 선호
            "top_p": 0.9,
            "num_predict": 768,   # 4개 태그 카테고리 + 감성 JSON 충분히 커버
        },
    }

    logger.debug("Ollama request payload: %s", json.dumps(payload, ensure_ascii=False))

    async with httpx.AsyncClient(timeout=httpx.Timeout(timeout)) as client:
        response = await client.post(f"{base_url}/api/chat", json=payload)
        response.raise_for_status()

    data = response.json()
    raw_content: str = data["message"]["content"]

    logger.debug("Ollama raw content: %s", raw_content)

    try:
        return json.loads(raw_content)
    except json.JSONDecodeError as exc:
        logger.error("Ollama 응답 JSON 파싱 실패. 원본: %s", raw_content)
        raise ValueError(f"Ollama가 유효하지 않은 JSON을 반환했습니다: {exc}") from exc
