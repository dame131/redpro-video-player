#!/usr/bin/env python3
"""Print the center of Android's system-ANR Wait button from a UI dump."""

import re
import sys
import xml.etree.ElementTree as ET


def main() -> int:
    if len(sys.argv) != 2:
        return 2
    root = ET.parse(sys.argv[1]).getroot()
    for node in root.iter("node"):
        if node.attrib.get("resource-id") != "android:id/aerr_wait":
            continue
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if not match:
            return 3
        left, top, right, bottom = map(int, match.groups())
        print((left + right) // 2, (top + bottom) // 2)
        return 0
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
