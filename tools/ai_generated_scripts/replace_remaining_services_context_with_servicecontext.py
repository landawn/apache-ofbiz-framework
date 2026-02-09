import re
from pathlib import Path

ROOT = Path(r"C:\Users\haiyangl\Landawn\apache-ofbiz-framework")
IMPORT_LINE = "import org.apache.ofbiz.model.ServiceContext;"
PATTERN = re.compile(r"Map\s*<\s*String\s*,\s*Object\s*>\s+context")

modified_files = 0
modified_occurrences = 0
for path in ROOT.rglob("*Services.java"):
    if "build" in path.parts or "target" in path.parts:
        continue
    text = path.read_text(encoding="utf-8")
    new_text, count = PATTERN.subn("ServiceContext context", text)
    if count == 0:
        continue

    if IMPORT_LINE not in new_text:
        pkg_match = re.search(r"(?m)^package\s+[^;]+;\s*$", new_text)
        if pkg_match:
            imports = list(re.finditer(r"(?m)^import\s+[^;]+;\s*$", new_text))
            if imports:
                pos = imports[-1].end()
                new_text = new_text[:pos] + "\n" + IMPORT_LINE + new_text[pos:]
            else:
                pos = pkg_match.end()
                new_text = new_text[:pos] + "\n\n" + IMPORT_LINE + new_text[pos:]

    path.write_text(new_text, encoding="utf-8", newline="")
    modified_files += 1
    modified_occurrences += count

print(f"modified_files={modified_files}")
print(f"modified_occurrences={modified_occurrences}")
