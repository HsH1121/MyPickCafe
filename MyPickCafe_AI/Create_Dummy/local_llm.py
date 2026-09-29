"""
더미 데이터 생성용 LLM 호출 — 로컬 Ollama 의 qwen2.5:14b 만 허용한다.

더미 생성은 리뷰 수만 건을 LLM 에 보내므로, 배포 환경(외부 유료 API · 서버 자원)에서
돌면 안 된다. 그래서 .env / 환경변수의 LLM_* 설정을 쓰지 않고 아래 값으로 고정하며,
첫 호출 전에 다음을 확인해 하나라도 어긋나면 프로세스를 멈춘다.

  1. 컨테이너 안이 아니다 (/.dockerenv 등)
  2. 프로세스 환경변수에 원격 LLM 설정이 없다
     (compose 가 배포 설정을 주입하는 곳 = 배포 환경. 로컬 .env 파일은 여기에 해당하지 않는다)
  3. 호출 주소가 loopback 이다 (OLLAMA_HOST 포함)
  4. 그 주소에서 응답하는 서버가 Ollama 이고, qwen2.5:14b 가 설치돼 있다

더미 생성 코드에서 LLM 을 부를 때는 반드시 이 모듈의 call_qwen() 을 쓴다.
ollama 라이브러리를 직접 쓰는 스크립트는 시작할 때 ensure_local_qwen() 을 부른다.
"""
from __future__ import annotations

import ipaddress
import os
import sys
from urllib.parse import urlparse

import httpx

_HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.abspath(os.path.join(_HERE, '..', 'Review_Tag_AI')))

from llm_client import call_llm  # noqa: E402

MODEL           = "qwen2.5:14b"
OLLAMA_BASE_URL = "http://127.0.0.1:11434/v1"
TIMEOUT         = 120

_CONTAINER_MARKERS = ("/.dockerenv", "/run/.containerenv")
_checked = False


class RemoteLLMRefused(SystemExit):
    """로컬 Ollama qwen 이 아닌 곳으로 호출하려 할 때."""


def _refuse(reason: str) -> None:
    raise RemoteLLMRefused(
        f"[중단] 더미 생성은 로컬 Ollama 의 {MODEL} 로만 실행할 수 있습니다.\n  이유: {reason}"
    )


def _is_loopback(url: str) -> bool:
    host = urlparse(url if "://" in url else f"http://{url}").hostname or ""
    if host == "localhost":
        return True
    try:
        return ipaddress.ip_address(host).is_loopback
    except ValueError:
        return False


def _in_container() -> bool:
    if any(os.path.exists(p) for p in _CONTAINER_MARKERS):
        return True
    try:
        with open("/proc/1/cgroup", encoding="utf-8") as f:
            return any(k in f.read() for k in ("docker", "containerd", "kubepods"))
    except OSError:
        return False


def ensure_local_qwen() -> None:
    """로컬 Ollama qwen 환경인지 확인한다. 아니면 RemoteLLMRefused 로 프로세스를 멈춘다."""
    global _checked
    if _checked:
        return

    if _in_container():
        _refuse("컨테이너 안에서 실행 중입니다(배포 환경).")

    env_url = os.environ.get("LLM_BASE_URL")
    if env_url and not _is_loopback(env_url):
        _refuse(f"환경변수 LLM_BASE_URL 이 원격 주소입니다({env_url}). 배포 환경으로 판단합니다.")
    if os.environ.get("LLM_API_KEY"):
        _refuse("환경변수 LLM_API_KEY 가 설정돼 있습니다. 배포 환경으로 판단합니다.")

    if not _is_loopback(OLLAMA_BASE_URL):
        _refuse(f"호출 주소가 loopback 이 아닙니다({OLLAMA_BASE_URL}).")
    ollama_host = os.environ.get("OLLAMA_HOST")
    if ollama_host and not _is_loopback(ollama_host):
        _refuse(f"환경변수 OLLAMA_HOST 가 원격 주소입니다({ollama_host}).")

    root = OLLAMA_BASE_URL.removesuffix("/v1")
    try:
        version = httpx.get(f"{root}/api/version", timeout=5).json().get("version")
        tags    = httpx.get(f"{root}/api/tags", timeout=5).json()
    except Exception as e:
        _refuse(f"로컬 Ollama({root})에 연결할 수 없습니다: {e}")
    if not version:
        _refuse(f"{root} 의 서버가 Ollama 가 아닙니다.")
    if MODEL not in {m.get("name") for m in tags.get("models", [])}:
        _refuse(f"로컬 Ollama 에 {MODEL} 이 없습니다. `ollama pull {MODEL}` 후 다시 실행하세요.")

    _checked = True


async def call_qwen(*, system_prompt: str, user_message: str, max_tokens: int = 1000) -> dict:
    """로컬 Ollama qwen 으로만 호출한다. 첫 호출 때 ensure_local_qwen() 을 거친다."""
    ensure_local_qwen()
    return await call_llm(
        system_prompt=system_prompt,
        user_message=user_message,
        model=MODEL,
        base_url=OLLAMA_BASE_URL,
        api_key=None,
        timeout=TIMEOUT,
        max_tokens=max_tokens,
    )
