from pathlib import Path
import re

ROOT = Path(r"C:\Users\haiyangl\Landawn\apache-ofbiz-framework")
PATTERN = re.compile(r"Map\s*<\s*String\s*,\s*\?\s*extends\s*Object\s*>\s+context")

matches = []
for file_path in ROOT.rglob("*Services.java"):
    text = file_path.read_text(encoding="utf-8")
    for idx, line in enumerate(text.splitlines(), start=1):
        if PATTERN.search(line):
            matches.append((file_path.relative_to(ROOT), idx, line.strip()))

print(f"matches={len(matches)}")
for file_path, line_no, line in matches:
    print(f"{file_path}:{line_no}:{line}")
