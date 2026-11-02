"""
내부 토큰 미들웨어(shared/internal_auth.py) 검증 — 서버·LLM·DB 없이 실행된다.

사용법 (MyPickCafe_AI/ 에서)
  python shared/test_internal_auth.py
"""

from __future__ import annotations
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
sys.stdout.reconfigure(encoding="utf-8")

from fastapi import FastAPI
from fastapi.testclient import TestClient

from shared.internal_auth import HEADER_NAME, install_internal_auth


def build_app(token: str) -> tuple[FastAPI, bool]:
    app = FastAPI()

    @app.get("/health")
    async def health() -> dict:
        return {"status": "ok"}

    @app.post("/pickbot/reindex")
    async def reindex() -> dict:
        return {"indexed": 0}

    installed = install_internal_auth(app, token)
    return app, installed


def main() -> int:
    failures = 0

    def check(name: str, ok: bool, detail: str = "") -> None:
        nonlocal failures
        failures += not ok
        print(f"[{'OK ' if ok else 'FAIL'}] {name}{(' — ' + detail) if detail else ''}")

    app, installed = build_app("s3cr3t")
    c = TestClient(app)
    check("토큰 설정 시 미들웨어 설치", installed)
    r = c.post("/pickbot/reindex")
    check("헤더 없으면 401", r.status_code == 401, str(r.status_code))
    r = c.post("/pickbot/reindex", headers={HEADER_NAME: "wrong"})
    check("틀린 토큰 401", r.status_code == 401, str(r.status_code))
    r = c.post("/pickbot/reindex", headers={HEADER_NAME: "s3cr3t"})
    check("맞는 토큰 200", r.status_code == 200, str(r.status_code))
    r = c.get("/health")
    check("/health 는 토큰 없이 200(컨테이너 헬스체크)", r.status_code == 200, str(r.status_code))
    r = c.get("/openapi.json")
    check("토큰 없이 /openapi.json 401", r.status_code == 401, str(r.status_code))

    app, installed = build_app("")
    c = TestClient(app)
    check("토큰이 비면 설치하지 않음", not installed)
    r = c.post("/pickbot/reindex")
    check("토큰이 비면 헤더 없이 200(로컬 개발 호환)", r.status_code == 200, str(r.status_code))

    print(f"\n실패 {failures}건")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
