import re
from pathlib import Path

ROOT = Path(r"C:\Users\haiyangl\Landawn\apache-ofbiz-framework")
for path in ROOT.rglob("*Services.java"):
    if "build" in path.parts or "target" in path.parts:
        continue
    text = path.read_text(encoding="utf-8")
    new_text = re.sub(r"\bServiceContext\s+context\b", "Map<String, Object> context", text)
    if new_text != text:
        path.write_text(new_text, encoding="utf-8", newline="")
print("done")
