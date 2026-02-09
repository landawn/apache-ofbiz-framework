import re
import subprocess
from pathlib import Path

ROOT = Path(r"C:\Users\haiyangl\Landawn\apache-ofbiz-framework")
X_PATH = ROOT / r"framework\entity\src\main\java\org\apache\ofbiz\persistence\entity\x.java"
MARKER = "// === Manually added fields ==="
X_PACKAGE = "org.apache.ofbiz.persistence.entity"
X_IMPORT = "import org.apache.ofbiz.persistence.entity.x;"
X_FQCN_PREFIX = "org.apache.ofbiz.persistence.entity.x."

JAVA_KEYWORDS = {
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const',
    'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float',
    'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native',
    'new', 'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp',
    'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void',
    'volatile', 'while'
}

CONST_PATTERN = re.compile(r'(?m)^\s*String\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*"((?:[^"\\]|\\.)*)";')
GV_DECL_PATTERN = re.compile(r'\b(?:org\.apache\.ofbiz\.entity\.)?GenericValue\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?=[=;,):])')
MAP_CONTEXT_DECL_PATTERN = re.compile(r'\bMap\s*<\s*String\s*,\s*([^>]+?)\s*>\s*(context)\s*(?=[=;,):])')
IMPORT_PATTERN = re.compile(r'(?m)^import\s+([^;]+);\s*$')
PACKAGE_PATTERN = re.compile(r'(?m)^package\s+([^;]+);\s*$')


def read_text_raw(path: Path) -> str:
    with path.open('r', encoding='utf-8', newline='') as f:
        return f.read()


def write_text_raw(path: Path, text: str) -> None:
    with path.open('w', encoding='utf-8', newline='') as f:
        f.write(text)


def detect_newline(text: str) -> str:
    return '\r\n' if '\r\n' in text else '\n'


def unescape_java_string(s: str) -> str:
    out = []
    i = 0
    n = len(s)
    while i < n:
        ch = s[i]
        if ch != '\\':
            out.append(ch)
            i += 1
            continue

        i += 1
        if i >= n:
            out.append('\\')
            break

        esc = s[i]
        if esc == 'b':
            out.append('\b')
            i += 1
        elif esc == 't':
            out.append('\t')
            i += 1
        elif esc == 'n':
            out.append('\n')
            i += 1
        elif esc == 'f':
            out.append('\f')
            i += 1
        elif esc == 'r':
            out.append('\r')
            i += 1
        elif esc == '"':
            out.append('"')
            i += 1
        elif esc == "'":
            out.append("'")
            i += 1
        elif esc == '\\':
            out.append('\\')
            i += 1
        elif esc == 'u':
            while i < n and s[i] == 'u':
                i += 1
            if i + 4 <= n:
                hex_part = s[i:i + 4]
                if re.fullmatch(r'[0-9a-fA-F]{4}', hex_part):
                    out.append(chr(int(hex_part, 16)))
                    i += 4
                else:
                    out.append('\\u')
            else:
                out.append('\\u')
        elif '0' <= esc <= '7':
            oct_digits = [esc]
            i += 1
            for _ in range(2):
                if i < n and '0' <= s[i] <= '7':
                    oct_digits.append(s[i])
                    i += 1
                else:
                    break
            out.append(chr(int(''.join(oct_digits), 8)))
        else:
            out.append(esc)
            i += 1

    return ''.join(out)


def escape_java_string(s: str) -> str:
    out = []
    for ch in s:
        code = ord(ch)
        if ch == '\\':
            out.append('\\\\')
        elif ch == '"':
            out.append('\\"')
        elif ch == '\n':
            out.append('\\n')
        elif ch == '\r':
            out.append('\\r')
        elif ch == '\t':
            out.append('\\t')
        elif ch == '\f':
            out.append('\\f')
        elif ch == '\b':
            out.append('\\b')
        elif code < 32:
            out.append(f'\\u{code:04x}')
        else:
            out.append(ch)
    return ''.join(out)


def build_code_mask(text: str):
    n = len(text)
    mask = [False] * n
    i = 0
    state = 'NORMAL'
    while i < n:
        ch = text[i]
        nxt = text[i + 1] if i + 1 < n else ''

        if state == 'NORMAL':
            if ch == '/' and nxt == '/':
                state = 'SL_COMMENT'
                i += 2
                continue
            if ch == '/' and nxt == '*':
                state = 'ML_COMMENT'
                i += 2
                continue
            if ch == '"':
                state = 'STRING'
                i += 1
                continue
            if ch == "'":
                state = 'CHAR'
                i += 1
                continue
            mask[i] = True
            i += 1
            continue

        if state == 'SL_COMMENT':
            if ch == '\n':
                state = 'NORMAL'
            i += 1
            continue

        if state == 'ML_COMMENT':
            if ch == '*' and nxt == '/':
                state = 'NORMAL'
                i += 2
            else:
                i += 1
            continue

        if state == 'STRING':
            if ch == '\\':
                i += 2
            elif ch == '"':
                state = 'NORMAL'
                i += 1
            else:
                i += 1
            continue

        if state == 'CHAR':
            if ch == '\\':
                i += 2
            elif ch == "'":
                state = 'NORMAL'
                i += 1
            else:
                i += 1
            continue

    return mask


def apply_pattern_in_code(text: str, pattern: re.Pattern, repl_func):
    mask = build_code_mask(text)
    out = []
    last = 0
    changed = 0
    for m in pattern.finditer(text):
        start = m.start()
        if start >= len(mask) or not mask[start]:
            continue
        repl = repl_func(m)
        if repl is None or repl == m.group(0):
            continue
        out.append(text[last:start])
        out.append(repl)
        last = m.end()
        changed += 1

    if changed == 0:
        return text, 0

    out.append(text[last:])
    return ''.join(out), changed


def has_code_x_field_usage(text: str) -> bool:
    mask = build_code_mask(text)
    p = re.compile(r'\bx\.[A-Za-z_][A-Za-z0-9_]*\b')
    for m in p.finditer(text):
        if m.start() < len(mask) and mask[m.start()]:
            return True
    return False


def ensure_x_import(text: str) -> str:
    if X_IMPORT in text:
        return text

    package_match = PACKAGE_PATTERN.search(text)
    if not package_match:
        return text

    package_name = package_match.group(1).strip()
    if package_name == X_PACKAGE:
        return text

    imports = list(IMPORT_PATTERN.finditer(text))
    nl = detect_newline(text)

    if imports:
        insert_pos = imports[-1].end()
        insertion = nl + X_IMPORT
        return text[:insert_pos] + insertion + text[insert_pos:]

    insert_pos = package_match.end()
    insertion = nl + nl + X_IMPORT
    return text[:insert_pos] + insertion + text[insert_pos:]


x_text = read_text_raw(X_PATH)
x_newline = detect_newline(x_text)

value_to_const = {}
used_const_names = set()

for m in CONST_PATTERN.finditer(x_text):
    const_name = m.group(1)
    literal = unescape_java_string(m.group(2))
    used_const_names.add(const_name)
    value_to_const.setdefault(literal, const_name)

new_constants = {}  # literal -> const name


def make_const_name(literal: str) -> str:
    base = re.sub(r'[^0-9A-Za-z_]', '_', literal)
    base = re.sub(r'_+', '_', base)
    if not base:
        base = 'field'
    if not re.match(r'[A-Za-z_]', base[0]):
        base = '_' + base
    if base in JAVA_KEYWORDS:
        base = base + '_'

    name = base
    idx = 2
    while name in used_const_names:
        name = f'{base}_{idx}'
        idx += 1
    return name


def ensure_const_for_literal(literal: str) -> str:
    if literal in value_to_const:
        return value_to_const[literal]

    if literal in new_constants:
        return new_constants[literal]

    name = make_const_name(literal)
    used_const_names.add(name)
    value_to_const[literal] = name
    new_constants[literal] = name
    return name


tracked_java_files = subprocess.check_output(
    ['git', 'ls-files', '*.java'],
    cwd=ROOT,
    text=True,
    encoding='utf-8'
).splitlines()

changed_files = []
replacement_count = 0
fqcn_to_simple_count = 0
imports_added_count = 0

for rel in tracked_java_files:
    path = ROOT / rel
    if path == X_PATH:
        continue

    text = read_text_raw(path)
    original = text

    gv_vars = set(GV_DECL_PATTERN.findall(text))
    if gv_vars:
        for var in sorted(gv_vars, key=lambda v: (-len(v), v)):
            call_pattern = re.compile(
                rf'(?<![A-Za-z0-9_$])((?:this\s*\.\s*)?{re.escape(var)}\s*\.\s*(?:get[A-Za-z0-9_]*|set[A-Za-z0-9_]*)\s*\(\s*)"((?:[^"\\]|\\.)*)"'
            )

            def repl_gv(m):
                literal = unescape_java_string(m.group(2))
                const_name = ensure_const_for_literal(literal)
                return f'{m.group(1)}x.{const_name}'

            text, c = apply_pattern_in_code(text, call_pattern, repl_gv)
            replacement_count += c

    has_context_map = False
    context_mask = build_code_mask(text)
    for m in MAP_CONTEXT_DECL_PATTERN.finditer(text):
        if not context_mask[m.start()]:
            continue
        value_type = ' '.join(m.group(1).replace('\r', ' ').replace('\n', ' ').split())
        value_type_l = value_type.lower()
        if value_type_l in {
            'object',
            'java.lang.object',
            '?',
            '? extends object',
            '? extends java.lang.object'
        }:
            has_context_map = True
            break

    if has_context_map:
        context_call_pattern = re.compile(
            r'(?<![A-Za-z0-9_$])((?:this\s*\.\s*)?context\s*\.\s*(?:get|put)\s*\(\s*)"((?:[^"\\]|\\.)*)"'
        )

        def repl_ctx(m):
            literal = unescape_java_string(m.group(2))
            const_name = ensure_const_for_literal(literal)
            return f'{m.group(1)}x.{const_name}'

        text, c = apply_pattern_in_code(text, context_call_pattern, repl_ctx)
        replacement_count += c

    fqcn_pattern = re.compile(r'org\.apache\.ofbiz\.persistence\.entity\.x\.([A-Za-z_][A-Za-z0-9_]*)')

    def repl_fqcn(m):
        return f'x.{m.group(1)}'

    text, c = apply_pattern_in_code(text, fqcn_pattern, repl_fqcn)
    fqcn_to_simple_count += c

    before_import = text
    if has_code_x_field_usage(text):
        text = ensure_x_import(text)
        if text != before_import:
            imports_added_count += 1

    if text != original:
        write_text_raw(path, text)
        changed_files.append(rel)

if new_constants:
    if MARKER not in x_text:
        raise RuntimeError(f'Marker not found in {X_PATH}')

    final_brace_idx = x_text.rfind('}')
    if final_brace_idx < 0:
        raise RuntimeError(f'Final closing brace not found in {X_PATH}')

    sorted_new = sorted(new_constants.items(), key=lambda kv: kv[1].lower())
    new_lines = [f'    String {name} = "{escape_java_string(literal)}";{x_newline}' for literal, name in sorted_new]

    insertion = ''.join(new_lines)

    prefix = x_text[:final_brace_idx]
    suffix = x_text[final_brace_idx:]

    if not prefix.endswith(x_newline):
        prefix += x_newline
    if not prefix.endswith(x_newline * 2):
        prefix += x_newline

    x_updated = prefix + insertion + x_newline + suffix
    write_text_raw(X_PATH, x_updated)

print(f'Changed files: {len(changed_files)}')
print(f'Replacements (string->x): {replacement_count}')
print(f'FQCN->x conversions: {fqcn_to_simple_count}')
print(f'Imports added: {imports_added_count}')
print(f'New x fields: {len(new_constants)}')
if changed_files:
    print('First changed files:')
    for rel in changed_files[:40]:
        print(rel)
if new_constants:
    print('First new x constants:')
    for literal, name in list(sorted(new_constants.items(), key=lambda kv: kv[1].lower()))[:40]:
        print(f'{name} = {literal}')
