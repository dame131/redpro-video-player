#!/usr/bin/env python3
"""Fail CI when an Android screen tour produced missing, blank, or duplicate images."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import sys

from PIL import Image, ImageStat


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("directory", type=Path)
    parser.add_argument("--required", nargs="*", default=[])
    args = parser.parse_args()
    images = sorted(args.directory.rglob("*.png")) if args.directory.exists() else []
    errors: list[str] = []
    if not images:
        errors.append("No screenshots were produced")
    names = {image.name for image in images}
    errors.extend(f"Missing required screenshot: {name}" for name in args.required if name not in names)
    hashes: dict[str, Path] = {}
    for path in images:
        try:
            with Image.open(path) as image:
                image.verify()
            with Image.open(path) as image:
                rgb = image.convert("RGB")
                if rgb.width < 320 or rgb.height < 480:
                    errors.append(f"Screenshot is too small: {path.name} ({rgb.width}x{rgb.height})")
                variation = sum(ImageStat.Stat(rgb.resize((64, 64))).stddev) / 3
                if variation < 1.0:
                    errors.append(f"Screenshot has almost no pixel variation: {path.name}")
        except Exception as exc:
            errors.append(f"Invalid PNG {path.name}: {exc}")
            continue
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        if digest in hashes:
            errors.append(f"Duplicate screenshots: {hashes[digest].name} and {path.name}")
        hashes[digest] = path
    if errors:
        print("\n".join(f"ERROR: {error}" for error in errors), file=sys.stderr)
        return 1
    print(f"Validated {len(images)} distinct screenshots")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
