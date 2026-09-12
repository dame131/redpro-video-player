#!/usr/bin/env python3
"""Fail when logcat attributes a crash or ANR to the tested Android package."""

from __future__ import annotations

import argparse
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("logcat", type=Path)
    parser.add_argument("package")
    args = parser.parse_args()
    lines = args.logcat.read_text(errors="replace").splitlines()
    errors: list[str] = []

    for index, line in enumerate(lines):
        if f"ANR in {args.package}" in line:
            errors.append(line.strip())
        if "FATAL EXCEPTION:" in line:
            crash_block = "\n".join(lines[index : index + 15])
            if f"Process: {args.package}" in crash_block:
                errors.append(line.strip())

    if errors:
        print("App crash or ANR detected:")
        print("\n".join(errors))
        return 1
    print(f"No crash or ANR attributed to {args.package}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
