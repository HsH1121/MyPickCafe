"""Run reviewed, service-free scripts in a disposable copy, never the live AI tree."""

from __future__ import annotations

import argparse
import compileall
import os
from pathlib import Path
import runpy
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET


OFFLINE_TESTS = (
    "test_condition_search.py",
    "test_parse_timeout.py",
    "test_pick_trace.py",
    "test_query_length.py",
    "test_region_filter.py",
    "test_region_map.py",
)


def run_worker(script: Path) -> int:
    # Fail even when application fallback code catches a blocked connection.
    # This guards Python sockets; it is not an OS-level network sandbox.
    network_attempts = []
    socketpair_code = getattr(socket.socketpair, "__code__", None)

    def block_network(event, args):
        # Windows implements asyncio's internal socketpair using a loopback
        # connection. Allow only the connect made directly by that stdlib function.
        if event == "socket.connect" and sys._getframe(1).f_code is socketpair_code:
            return
        if event in {"socket.connect", "socket.getaddrinfo", "socket.sendto"}:
            network_attempts.append(event)
            raise RuntimeError("Offline CI test attempted a network connection")

    sys.addaudithook(block_network)
    exit_code = 0
    try:
        runpy.run_path(str(script), run_name="__main__")
    except SystemExit as exc:
        exit_code = exc.code if isinstance(exc.code, int) else (1 if exc.code else 0)
    if network_attempts:
        print("FAIL: network access attempted during an offline test", file=sys.stderr)
        return 1
    return exit_code


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--worker", type=Path, help=argparse.SUPPRESS)
    args = parser.parse_args()
    if args.worker:
        return run_worker(args.worker)

    repo = Path(__file__).resolve().parents[2]
    source = repo / "MyPickCafe_AI"
    reports = repo / "MyPickCafe_Springboot/build/reports/ai"
    reports.mkdir(parents=True, exist_ok=True)
    suite = ET.Element("testsuite", name="ai-offline-scripts")
    failures = 0

    with tempfile.TemporaryDirectory(prefix="mypickcafe-ci-") as tmp:
        isolated = Path(tmp) / "MyPickCafe_AI"
        isolated.mkdir()
        # Copy only application sources/resources. Never copy .env, existing logs,
        # ChromaDB files, virtualenvs, or external-service test scripts.
        shutil.copy2(source / "app.py", isolated / "app.py")
        for package in ("PickBot_AI", "Review_Tag_AI", "shared"):
            target = isolated / package
            target.mkdir()
            for path in (source / package).iterdir():
                if path.is_file() and path.suffix in {".py", ".json"}:
                    shutil.copy2(path, target / path.name)
        tests = isolated / "PickBot_AI/tests"
        tests.mkdir()
        for name in OFFLINE_TESTS:
            shutil.copy2(source / "PickBot_AI/tests" / name, tests / name)

        compiled = compileall.compile_dir(str(isolated), quiet=1)
        case = ET.SubElement(suite, "testcase", name="compile-application-sources")
        if not compiled:
            ET.SubElement(case, "failure", message="Python compilation failed")
            failures += 1

        env = os.environ.copy()
        # Discard application settings inherited from a developer's shell.
        for key in list(env):
            if key.upper().startswith(("LLM_", "DB_", "EMBED_", "CHROMA_", "PICKBOT_", "REGION_", "PYTHON")):
                del env[key]
        env.update(
            PYTHONUTF8="1", PYTHONDONTWRITEBYTECODE="1", ANONYMIZED_TELEMETRY="False",
            LLM_BASE_URL="http://127.0.0.1:1", LLM_API_KEY="ci-unused",
            LLM_MODEL="ci-unused", EMBED_BASE_URL="http://127.0.0.1:1",
            DB_HOST="127.0.0.1", DB_PORT="1", DB_NAME="ci_unused",
            DB_USER="ci_unused", DB_PASSWORD="ci-unused", PICKBOT_TRACE="false",
            CHROMA_PATH=str(isolated / "chroma_db"),
            REGION_CANDIDATES_PATH=str(isolated / "candidates.json"),
        )
        for name in OFFLINE_TESTS:
            started = time.monotonic()
            try:
                result = subprocess.run(
                    [sys.executable, str(Path(__file__).resolve()), "--worker", str(tests / name)],
                    cwd=isolated, env=env, capture_output=True, text=True,
                    encoding="utf-8", errors="replace", timeout=120, check=False,
                )
                output = result.stdout + result.stderr
                code = result.returncode
            except subprocess.TimeoutExpired as exc:
                output = "Timed out after 120 seconds\n"
                for part in (exc.stdout, exc.stderr):
                    if part:
                        output += part.decode("utf-8", errors="replace") if isinstance(part, bytes) else part
                code = 1
            elapsed = time.monotonic() - started
            (reports / f"{name}.log").write_text(output, encoding="utf-8")
            print(f"{'PASS' if code == 0 else 'FAIL'} {name} ({elapsed:.1f}s)", flush=True)
            print(output, flush=True)
            case = ET.SubElement(suite, "testcase", name=name, time=f"{elapsed:.3f}")
            ET.SubElement(case, "system-out").text = output
            if code:
                failures += 1
                ET.SubElement(case, "failure", message=f"Script exited with code {code}").text = output

    suite.set("tests", str(len(suite)))
    suite.set("failures", str(failures))
    ET.ElementTree(suite).write(reports / "junit.xml", encoding="utf-8", xml_declaration=True)
    print(f"AI CI: {len(suite)} checks, {failures} failures. Reports: {reports}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
