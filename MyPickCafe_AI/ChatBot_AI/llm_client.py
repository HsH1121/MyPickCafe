"""
공유 LLM 클라이언트 재노출 모듈 (실제 구현 없음).

실제 구현은 ../shared/llm_client.py 한 곳에만 있습니다.
동작을 바꾸려면 이 파일이 아니라 shared/llm_client.py 를 수정하세요.
ChatBot_AI / Review_Tag_AI 양쪽에 그대로 반영됩니다.

이 파일과 공유 구현이 둘 다 llm_client 라는 이름이라 sys.path 로는 서로를 가려
불러올 수 없습니다. 그래서 공유 구현은 파일 경로로 직접, 겹치지 않는 이름으로 로드합니다.
"""

from __future__ import annotations
import importlib.util
import os
import sys

_SHARED_NAME = "shared_llm_client"
_SHARED_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "shared",
    "llm_client.py",
)

_shared = sys.modules.get(_SHARED_NAME)
if _shared is None:
    _spec = importlib.util.spec_from_file_location(_SHARED_NAME, _SHARED_PATH)
    _shared = importlib.util.module_from_spec(_spec)
    sys.modules[_SHARED_NAME] = _shared
    try:
        _spec.loader.exec_module(_shared)
    except BaseException:
        sys.modules.pop(_SHARED_NAME, None)
        raise

call_llm = _shared.call_llm

__all__ = ["call_llm"]
