#!/usr/bin/env python3
"""Scan PriceServices classes for raw Map context method signatures."""

from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
PATTERN = re.compile(r"Map<String,\s*\?\s*extends\s*Object>\s+context")


def main() -> int:
    java_files = sorted(ROOT.rglob("*PriceServices*.java"))
    matches = []

    for file_path in java_files:
        text = file_path.read_text(encoding="utf-8", errors="ignore")
        for idx, line in enumerate(text.splitlines(), start=1):
            if PATTERN.search(line):
                matches.append((file_path.relative_to(ROOT), idx, line.strip()))

    if not matches:
        print("No wildcard-Map context signatures found in *PriceServices*.java")
        return 0

    for rel_path, line_no, line in matches:
        print(f"{rel_path}:{line_no}: {line}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
