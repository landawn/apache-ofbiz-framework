#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import keyword
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
X_JAVA = ROOT / "framework/entity/src/main/java/org/apache/ofbiz/persistence/entity/x.java"
X_IMPORT = "import org.apache.ofbiz.persistence.entity.x;"
MANUAL_MARKER = "// === Manually added fields ==="


JAVA_KEYWORDS = {
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while",
    "true",
    "false",
    "null",
}


def java_unescape(raw: str) -> str:
    out: list[str] = []
    i = 0
    while i < len(raw):
        ch = raw[i]
        if ch != "\\":
            out.append(ch)
            i += 1
            continue
        if i + 1 >= len(raw):
            out.append("\\")
            break
        i += 1
        esc = raw[i]
        if esc == "n":
            out.append("\n")
        elif esc == "r":
            out.append("\r")
        elif esc == "t":
            out.append("\t")
        elif esc == "b":
            out.append("\b")
        elif esc == "f":
            out.append("\f")
        elif esc in {'"', "'", "\\"}:
            out.append(esc)
        elif esc == "u":
            u = i + 1
            while u < len(raw) and raw[u] == "u":
                u += 1
            hex_part = raw[u : u + 4]
            if len(hex_part) == 4 and re.fullmatch(r"[0-9a-fA-F]{4}", hex_part):
                out.append(chr(int(hex_part, 16)))
                i = u + 3
            else:
                out.append("\\u")
        elif esc in "01234567":
            octal = esc
            j = i + 1
            for _ in range(2):
                if j < len(raw) and raw[j] in "01234567":
                    octal += raw[j]
                    j += 1
                else:
                    break
            out.append(chr(int(octal, 8)))
            i = j - 1
        else:
            out.append(esc)
        i += 1
    return "".join(out)


def java_escape(val: str) -> str:
    val = val.replace("\\", "\\\\")
    val = val.replace('"', '\\"')
    val = val.replace("\n", "\\n")
    val = val.replace("\r", "\\r")
    val = val.replace("\t", "\\t")
    val = val.replace("\b", "\\b")
    val = val.replace("\f", "\\f")
    return val


def parse_x_fields(text: str) -> tuple[dict[str, str], set[str]]:
    value_to_field: dict[str, str] = {}
    fields: set[str] = set()
    field_re = re.compile(r'^\s*String\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*"((?:\\.|[^"\\])*)";')
    for line in text.splitlines():
        m = field_re.match(line)
        if not m:
            continue
        field = m.group(1)
        val = java_unescape(m.group(2))
        fields.add(field)
        value_to_field.setdefault(val, field)
    return value_to_field, fields


def sanitize_base(value: str) -> str:
    if value == "":
        return "emptyString"
    base = re.sub(r"[^A-Za-z0-9_]", "_", value)
    base = re.sub(r"_+", "_", base).strip("_")
    if not base:
        base = "str"
    if re.match(r"^[0-9]", base):
        base = "_" + base
    if base in JAVA_KEYWORDS or keyword.iskeyword(base):
        base = "_" + base
    return base


def make_field_name(value: str, used_fields: set[str]) -> str:
    base = sanitize_base(value)
    if base not in used_fields:
        used_fields.add(base)
        return base
    suffix = hashlib.sha1(value.encode("utf-8")).hexdigest()[:8]
    candidate = f"{base}_{suffix}"
    if candidate not in used_fields:
        used_fields.add(candidate)
        return candidate
    i = 2
    while True:
        candidate = f"{base}_{suffix}_{i}"
        if candidate not in used_fields:
            used_fields.add(candidate)
            return candidate
        i += 1


def find_string_literals(src: str) -> list[tuple[int, int, str]]:
    NORMAL, SL_COMMENT, ML_COMMENT, STRING, CHAR, TEXTBLOCK = range(6)
    state = NORMAL
    i = 0
    results: list[tuple[int, int, str]] = []
    str_start = -1
    while i < len(src):
        ch = src[i]
        nxt = src[i + 1] if i + 1 < len(src) else ""
        if state == NORMAL:
            if ch == "/" and nxt == "/":
                state = SL_COMMENT
                i += 2
                continue
            if ch == "/" and nxt == "*":
                state = ML_COMMENT
                i += 2
                continue
            if ch == '"' and src[i : i + 3] == '"""':
                state = TEXTBLOCK
                i += 3
                continue
            if ch == '"':
                state = STRING
                str_start = i
                i += 1
                continue
            if ch == "'":
                state = CHAR
                i += 1
                continue
            i += 1
            continue
        if state == SL_COMMENT:
            if ch == "\n":
                state = NORMAL
            i += 1
            continue
        if state == ML_COMMENT:
            if ch == "*" and nxt == "/":
                state = NORMAL
                i += 2
                continue
            i += 1
            continue
        if state == CHAR:
            if ch == "\\":
                i += 2
                continue
            if ch == "'":
                state = NORMAL
            i += 1
            continue
        if state == TEXTBLOCK:
            if src[i : i + 3] == '"""':
                state = NORMAL
                i += 3
                continue
            i += 1
            continue
        if state == STRING:
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                raw = src[str_start + 1 : i]
                results.append((str_start, i + 1, java_unescape(raw)))
                state = NORMAL
                i += 1
                continue
            i += 1
            continue
    return results


def ensure_x_import(src: str) -> str:
    if X_IMPORT in src:
        return src
    lines = src.splitlines(keepends=True)
    import_indices = [idx for idx, ln in enumerate(lines) if ln.startswith("import ")]
    if import_indices:
        insert_at = import_indices[-1] + 1
        lines.insert(insert_at, X_IMPORT + "\n")
        return "".join(lines)
    package_idx = next((idx for idx, ln in enumerate(lines) if ln.startswith("package ")), None)
    if package_idx is not None:
        lines.insert(package_idx + 1, "\n")
        lines.insert(package_idx + 2, X_IMPORT + "\n")
        return "".join(lines)
    return X_IMPORT + "\n" + src


def list_service_files() -> list[Path]:
    files: list[Path] = []
    for base in (ROOT / "applications", ROOT / "framework"):
        if not base.exists():
            continue
        files.extend(sorted(base.rglob("*Services.java")))
    return files


def insert_new_fields(x_text: str, new_fields: list[tuple[str, str]]) -> str:
    if not new_fields:
        return x_text
    if MANUAL_MARKER not in x_text:
        raise RuntimeError(f"Marker not found in x.java: {MANUAL_MARKER}")
    insert_at = x_text.rfind("\n}")
    if insert_at < 0:
        raise RuntimeError("Unable to find closing brace in x.java")
    block = "".join(f'    String {name} = "{java_escape(value)}";\n' for name, value in new_fields)
    if not x_text[:insert_at].endswith("\n"):
        block = "\n" + block
    return x_text[:insert_at] + block + x_text[insert_at:]


def main() -> None:
    x_text = X_JAVA.read_text(encoding="utf-8")
    value_to_field, used_fields = parse_x_fields(x_text)
    new_fields: list[tuple[str, str]] = []

    files_changed = 0
    literals_replaced = 0

    for path in list_service_files():
        src = path.read_text(encoding="utf-8")
        literals = find_string_literals(src)
        if not literals:
            continue

        out: list[str] = []
        last = 0
        replaced_in_file = 0
        for start, end, val in literals:
            field = value_to_field.get(val)
            if field is None:
                field = make_field_name(val, used_fields)
                value_to_field[val] = field
                new_fields.append((field, val))
            out.append(src[last:start])
            out.append(f"x.{field}")
            last = end
            replaced_in_file += 1
        out.append(src[last:])
        new_src = "".join(out)
        if replaced_in_file > 0:
            new_src = ensure_x_import(new_src)
        if new_src != src:
            path.write_text(new_src, encoding="utf-8")
            files_changed += 1
            literals_replaced += replaced_in_file

    new_x = insert_new_fields(x_text, new_fields)
    if new_x != x_text:
        X_JAVA.write_text(new_x, encoding="utf-8")

    print(f"files_changed={files_changed}")
    print(f"literals_replaced={literals_replaced}")
    print(f"new_x_fields={len(new_fields)}")


if __name__ == "__main__":
    main()
