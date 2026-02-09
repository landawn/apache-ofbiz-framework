import re
from pathlib import Path

ROOT = Path(r"C:\Users\haiyangl\Landawn\apache-ofbiz-framework")
PATTERN = re.compile(r"Map\s*<\s*String\s*,\s*Object\s*>\s+context")
REPLACEMENT = "Map<String, ?> context"

modified_files = []
modified_count = 0
for path in ROOT.rglob("*Services.java"):
    if "build" in path.parts or "target" in path.parts:
        continue
    text = path.read_text(encoding="utf-8")
    new_text, count = PATTERN.subn(REPLACEMENT, text)
    if count:
        path.write_text(new_text, encoding="utf-8", newline="")
        modified_files.append(path)
        modified_count += count

print(f"modified_files={len(modified_files)}")
print(f"modified_occurrences={modified_count}")
