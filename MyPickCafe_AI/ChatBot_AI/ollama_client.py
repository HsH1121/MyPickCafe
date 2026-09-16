"""
공유 LLM 클라이언트 재노출 모듈 (실제 구현 없음).

실제 구현은 ../shared/llm_client.py 한 곳에만 있습니다.
동작을 바꾸려면 이 파일이 아니라 shared/llm_client.py 를 수정하세요.
ChatBot_AI / Review_Tag_AI 양쪽에 그대로 반영됩니다.
"""

from __future__ import annotations
import os
import sys

_SHARED_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "shared",
)
if _SHARED_DIR not in sys.path:
    sys.path.insert(0, _SHARED_DIR)

from llm_client import call_ollama  # noqa: E402,F401

__all__ = ["call_ollama"]
