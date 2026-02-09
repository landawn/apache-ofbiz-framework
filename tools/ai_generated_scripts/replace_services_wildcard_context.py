import re
from pathlib import Path

ROOT = Path(r"C:\Users\haiyangl\Landawn\apache-ofbiz-framework")
TARGET_GLOB = "*Services.java"

# Replace any wildcard-extends context map signature with invariant map signature.
PATTERN = re.compile(r"Map\s*<\s*String\s*,\s*\?\s*extends\s*Object\s*>\s+context")
REPLACEMENT = "Map<String, Object> context"

modified_files = []
modified_count = 0

for path in ROOT.rglob(TARGET_GLOB):
    if "build" in path.parts or "target" in path.parts:
        continue

    text = path.read_text(encoding="utf-8")
    new_text, count = PATTERN.subn(REPLACEMENT, text)
    if count > 0:
        path.write_text(new_text, encoding="utf-8", newline="")
        modified_files.append(path)
        modified_count += count

print(f"modified_files={len(modified_files)}")
print(f"modified_occurrences={modified_count}")
for file_path in modified_files:
    print(file_path.relative_to(ROOT))
