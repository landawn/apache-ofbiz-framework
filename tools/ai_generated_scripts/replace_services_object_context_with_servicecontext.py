import re
from pathlib import Path

ROOT = Path(r"C:\Users\haiyangl\Landawn\apache-ofbiz-framework")
TARGET_GLOB = "*Services.java"

SIGNATURE_PATTERN = re.compile(r"Map\s*<\s*String\s*,\s*Object\s*>\s+context")
IMPORT_LINE = "import org.apache.ofbiz.model.ServiceContext;"

modified_files = []
modified_occurrences = 0

for path in ROOT.rglob(TARGET_GLOB):
    if "build" in path.parts or "target" in path.parts:
        continue

    text = path.read_text(encoding="utf-8")
    new_text, count = SIGNATURE_PATTERN.subn("ServiceContext context", text)
    if count == 0:
        continue

    if IMPORT_LINE not in new_text:
        package_match = re.search(r"(?m)^package\s+[^;]+;\s*$", new_text)
        if package_match:
            imports = list(re.finditer(r"(?m)^import\s+[^;]+;\s*$", new_text))
            if imports:
                insert_pos = imports[-1].end()
                new_text = new_text[:insert_pos] + "\n" + IMPORT_LINE + new_text[insert_pos:]
            else:
                insert_pos = package_match.end()
                new_text = new_text[:insert_pos] + "\n\n" + IMPORT_LINE + new_text[insert_pos:]

    path.write_text(new_text, encoding="utf-8", newline="")
    modified_files.append(path)
    modified_occurrences += count

print(f"modified_files={len(modified_files)}")
print(f"modified_occurrences={modified_occurrences}")
for file_path in modified_files:
    print(file_path.relative_to(ROOT))
