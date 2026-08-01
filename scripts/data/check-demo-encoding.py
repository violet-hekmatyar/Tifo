#!/usr/bin/env python3
"""Verify T14 seed bytes and saved HTTP response bytes without console decoding."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path


def check_seed(path: Path) -> None:
    raw = path.read_bytes()
    text = raw.decode("utf-8")
    checks = {
        "utf8_valid": True,
        "no_bom": not raw.startswith(b"\xef\xbb\xbf"),
        "has_cjk": sum("\u4e00" <= char <= "\u9fff" for char in text) > 1000,
        "no_question_runs": re.search(r"\?{2,}", text) is None,
    }
    for name, passed in checks.items():
        print(f"{name}: {'PASS' if passed else 'FAIL'}")
    print("seed_cjk_count:", sum("\u4e00" <= char <= "\u9fff" for char in text))
    print("seed_sha256:", hashlib.sha256(raw).hexdigest())
    if not all(checks.values()):
        raise SystemExit(1)


def check_http(directory: Path) -> None:
    failed = []
    for path in sorted(directory.glob("*.json")):
        raw = path.read_bytes()
        text = raw.decode("utf-8")
        json.loads(text)
        cjk_count = sum("\u4e00" <= char <= "\u9fff" for char in text)
        question_runs = len(re.findall(r"\?{2,}", text))
        headers = path.with_suffix(".headers").read_text(encoding="iso-8859-1")
        content_type = next(
            (line.strip() for line in headers.splitlines() if line.lower().startswith("content-type:")),
            "",
        )
        passed = cjk_count > 0 and question_runs == 0 and "application/json" in content_type.lower()
        print(
            f"{path.name}: utf8=true cjk={cjk_count} question_runs={question_runs} "
            f"{content_type} {'PASS' if passed else 'FAIL'}"
        )
        if not passed:
            failed.append(path.name)
    if failed:
        raise SystemExit("HTTP encoding failed: " + ", ".join(failed))


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    seed = subparsers.add_parser("seed")
    seed.add_argument("path", type=Path)
    http = subparsers.add_parser("http")
    http.add_argument("directory", type=Path)
    args = parser.parse_args()
    if args.command == "seed":
        check_seed(args.path)
    else:
        check_http(args.directory)


if __name__ == "__main__":
    main()
