#!/usr/bin/env python3
"""
Refactor service method context parameters in *Services.java files.

Replaces method parameters of type:
  - ServiceContext context
  - Map<String, ? extends Object> context
  - Map<String, Object> context

with generated typed context classes in org.apache.ofbiz.model.
"""

from __future__ import annotations

import re
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MODEL_DIR = ROOT / "framework" / "base" / "src" / "main" / "java" / "org" / "apache" / "ofbiz" / "model"

PARAM_PATTERN = re.compile(
    r"([,(]\s*)(?:"
    r"ServiceContext|"
    r"Map\s*<\s*String\s*,\s*(?:\?\s*extends\s*Object|Object)\s*>"
    r")\s+context(?=\s*[),])"
)

PACKAGE_PATTERN = re.compile(r"^\s*package\s+([a-zA-Z0-9_.]+)\s*;\s*$", re.MULTILINE)
SERVICE_PARAM_SCAN = re.compile(
    r"\b(?:ServiceContext|Map\s*<\s*String\s*,\s*(?:\?\s*extends\s*Object|Object)\s*>)\s+context\b"
)


def _camel(segment: str) -> str:
    parts = re.split(r"[^0-9A-Za-z]+", segment)
    return "".join(p[:1].upper() + p[1:] for p in parts if p)


def _detect_newline(text: str) -> str:
    return "\r\n" if "\r\n" in text else "\n"


def _find_service_files() -> list[Path]:
    candidates: list[Path] = []
    for base in (ROOT / "applications", ROOT / "framework"):
        if not base.exists():
            continue
        candidates.extend(sorted(base.rglob("*Services.java")))
    return candidates


def _read_package(java_text: str) -> str:
    match = PACKAGE_PATTERN.search(java_text)
    if not match:
        raise ValueError("Missing package declaration")
    return match.group(1)


def _build_context_name(package_name: str, class_name: str, used_names: set[str]) -> str:
    base_name = f"{class_name}Context"
    if base_name not in used_names:
        used_names.add(base_name)
        return base_name

    pkg_parts = [p for p in package_name.split(".") if p not in {"org", "apache", "ofbiz"}]
    prefix = "".join(_camel(p) for p in pkg_parts)
    if prefix:
        candidate = f"{prefix}{class_name}Context"
        if candidate not in used_names:
            used_names.add(candidate)
            return candidate

    idx = 2
    while True:
        candidate = f"{base_name}{idx}"
        if candidate not in used_names:
            used_names.add(candidate)
            return candidate
        idx += 1


def _insert_import(text: str, import_line: str, newline: str) -> str:
    if import_line in text:
        return text

    lines = text.splitlines()
    insert_idx = None
    for i, line in enumerate(lines):
        if line.startswith("import "):
            insert_idx = i + 1
    if insert_idx is None:
        for i, line in enumerate(lines):
            if line.startswith("package "):
                insert_idx = i + 1
                break
    if insert_idx is None:
        insert_idx = 0

    lines.insert(insert_idx, import_line)
    return newline.join(lines) + (newline if text.endswith(("\n", "\r")) else "")


def _remove_unused_service_context_import(text: str) -> str:
    if "ServiceContext" in text:
        return text
    return re.sub(r"^import org\.apache\.ofbiz\.model\.ServiceContext;\s*\r?\n", "", text, flags=re.MULTILINE)


def _write_context_class(class_name: str) -> None:
    target = MODEL_DIR / f"{class_name}.java"
    if target.exists():
        return
    source = (
        "package org.apache.ofbiz.model;\n\n"
        "import java.util.Map;\n\n"
        "public class "
        + class_name
        + " extends ServiceContext {\n"
        "    private static final long serialVersionUID = 1L;\n\n"
        "    public "
        + class_name
        + "() {\n"
        "        super();\n"
        "    }\n\n"
        "    public "
        + class_name
        + "(Map<String, ?> source) {\n"
        "        super(source);\n"
        "    }\n"
        "}\n"
    )
    target.write_text(source, encoding="utf-8", newline="\n")


def main() -> None:
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    service_files = _find_service_files()
    if not service_files:
        print("No service files found")
        return

    existing_names = {p.stem for p in MODEL_DIR.glob("*.java")}
    file_to_context: dict[Path, str] = {}
    changed_files: list[Path] = []
    generated_classes: set[str] = set()
    conflicts = defaultdict(list)

    for file_path in service_files:
        text = file_path.read_text(encoding="utf-8")
        if not SERVICE_PARAM_SCAN.search(text):
            continue
        package_name = _read_package(text)
        class_name = file_path.stem
        context_name = _build_context_name(package_name, class_name, existing_names)
        file_to_context[file_path] = context_name
        conflicts[class_name].append(context_name)

    for file_path, context_name in file_to_context.items():
        text = file_path.read_text(encoding="utf-8")
        newline = _detect_newline(text)
        updated = PARAM_PATTERN.sub(rf"\1{context_name} context", text)
        if updated == text:
            continue

        import_line = f"import org.apache.ofbiz.model.{context_name};"
        updated = _insert_import(updated, import_line, newline)
        updated = _remove_unused_service_context_import(updated)

        file_path.write_text(updated, encoding="utf-8", newline="")
        changed_files.append(file_path)
        generated_classes.add(context_name)

    for ctx_name in sorted(generated_classes):
        _write_context_class(ctx_name)

    print(f"Service files scanned: {len(service_files)}")
    print(f"Service files changed: {len(changed_files)}")
    print(f"Context classes generated: {len(generated_classes)}")
    if conflicts:
        dupes = {k: v for k, v in conflicts.items() if len(v) > 1}
        if dupes:
            print(f"Duplicate service class names resolved: {len(dupes)}")
            for key in sorted(dupes):
                print(f"  {key}: {', '.join(sorted(dupes[key]))}")


if __name__ == "__main__":
    main()
