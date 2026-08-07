#!/usr/bin/env python3
"""Convert RagChunk Java sources to Lombok style."""
import re
import os
from pathlib import Path

ROOT = Path(r"d:\codework\RagChunk\src\main\java\com\xtsh\ragchunk")

SKIP_FILES = set()  # filled during processing
changed = []
skipped = []

LOMBOK_IMPORTS = {
    "Data": "lombok.Data",
    "Slf4j": "lombok.extern.slf4j.Slf4j",
    "RequiredArgsConstructor": "lombok.RequiredArgsConstructor",
    "NoArgsConstructor": "lombok.NoArgsConstructor",
}

SPRING_STEREOTYPES = ("@Service", "@Component", "@RestController", "@Repository", "@Controller")

def read_text(p: Path) -> str:
    return p.read_text(encoding="utf-8")

def write_text(p: Path, s: str) -> None:
    p.write_text(s, encoding="utf-8", newline="\n")

def is_interface(content: str) -> bool:
    return bool(re.search(r"^\s*public\s+interface\s+\w+", content, re.M))

def is_enum_only(content: str) -> bool:
    return bool(re.search(r"^\s*public\s+enum\s+\w+", content, re.M)) and " class " not in content.replace("record", "")

def is_top_level_record(content: str) -> bool:
    m = re.search(r"^\s*public\s+record\s+\w+", content, re.M)
    if not m:
        return False
    # top-level record file if no public class besides nested
    if re.search(r"^\s*public\s+class\s+\w+", content, re.M):
        return False
    return True

def skip_reason(content: str, path: Path) -> str | None:
    if is_interface(content):
        return "interface"
    name = path.name
    if name.endswith("Exception.java"):
        return "exception"
    if is_enum_only(content):
        return "enum"
    if is_top_level_record(content):
        return "top-level record"
    return None

def find_record_regions(content: str) -> list[tuple[int, int]]:
    """Return (start, end) char indices of record declarations including body."""
    regions = []
    for m in re.finditer(r"\brecord\s+\w+", content):
        # find opening brace after record header
        i = m.end()
        while i < len(content) and content[i] not in "{;":
            i += 1
        if i >= len(content) or content[i] == ";":
            continue
        depth = 0
        start = i
        while i < len(content):
            if content[i] == "{":
                depth += 1
            elif content[i] == "}":
                depth -= 1
                if depth == 0:
                    regions.append((m.start(), i + 1))
                    break
            i += 1
    return regions

def in_regions(pos: int, regions: list[tuple[int, int]]) -> bool:
    return any(a <= pos < b for a, b in regions)

def remove_trivial_accessors(content: str) -> tuple[str, bool]:
    record_regions = find_record_regions(content)
    modified = False
    i = 0
    out = []
    n = len(content)

    accessor_re = re.compile(
        r"(\n[ \t]*)(public|protected)\s+"
        r"([\w<>,\[\]\s.?@]+?)\s+"
        r"(get|set|is)([A-Z]\w*)\s*"
        r"\(([^)]*)\)\s*"
    )

    while i < n:
        if in_regions(i, record_regions):
            out.append(content[i])
            i += 1
            continue
        m = accessor_re.match(content, i)
        if not m:
            out.append(content[i])
            i += 1
            continue
        # possible method - find body
        j = m.end()
        while j < n and content[j] in " \t":
            j += 1
        if j >= n or content[j] != "{":
            out.append(content[i])
            i += 1
            continue
        # balanced brace body
        depth = 0
        body_start = j
        k = j
        while k < n:
            if content[k] == "{":
                depth += 1
            elif content[k] == "}":
                depth -= 1
                if depth == 0:
                    body = content[body_start + 1 : k]
                    if is_trivial_accessor_body(body):
                        i = k + 1
                        # swallow trailing newline
                        while i < n and content[i] in "\r\n":
                            i += 1
                        modified = True
                        break
                    else:
                        out.append(content[i])
                        i += 1
                        break
            k += 1
        else:
            out.append(content[i])
            i += 1
    return "".join(out), modified

def is_trivial_accessor_body(body: str) -> bool:
    lines = []
    for line in body.splitlines():
        s = line.strip()
        if not s or s.startswith("//") or s.startswith("*") or s.startswith("/*") or s.startswith("*/"):
            continue
        lines.append(s)
    if len(lines) != 1:
        return False
    line = lines[0]
    if line.startswith("return ") and line.endswith(";"):
        return True
    if line.startswith("this.") and "=" in line and line.endswith(";"):
        return True
    return False

def add_import(content: str, lombok_ann: str) -> str:
    imp = LOMBOK_IMPORTS[lombok_ann]
    if f"import {imp};" in content:
        return content
    # after package
    pkg_m = re.match(r"(package\s+[\w.]+;\s*\n)", content)
    if pkg_m:
        insert_at = pkg_m.end()
        return content[:insert_at] + f"import {imp};\n" + content[insert_at:]
    return f"import {imp};\n" + content

def ensure_imports(content: str, anns: set[str]) -> str:
    for a in sorted(anns):
        content = add_import(content, a)
    return content

def remove_imports(content: str, patterns: list[str]) -> str:
    for pat in patterns:
        content = re.sub(rf"^import {re.escape(pat)};\s*\n", "", content, flags=re.M)
    return content

def apply_slf4j(content: str) -> tuple[str, bool]:
    if "LoggerFactory.getLogger" not in content:
        return content, False
    # remove logger field lines
    new = re.sub(
        r"\n[ \t]*private\s+static\s+final\s+Logger\s+log\s*=\s*LoggerFactory\.getLogger\([^)]+\);\s*\n",
        "\n",
        content,
    )
    new = re.sub(
        r"\n[ \t]*private\s+final\s+Logger\s+log\s*=\s*LoggerFactory\.getLogger\([^)]+\);\s*\n",
        "\n",
        new,
    )
    new = remove_imports(new, ["org.slf4j.Logger", "org.slf4j.LoggerFactory"])
    # add @Slf4j before class
    if "@Slf4j" not in new:
        new = re.sub(
            r"(\n)(@(?:Service|Component|RestController|Repository|Controller)[^\n]*\n)?(\s*public\s+class\s+)",
            r"\1\2@Slf4j\n\3",
            new,
            count=1,
        )
        if "@Slf4j" not in new:
            new = re.sub(r"(\n)(\s*public\s+class\s+)", r"\1@Slf4j\n\2", new, count=1)
    return new, new != content or "@Slf4j" in new

def constructor_only_assigns(body: str) -> bool:
    lines = []
    for line in body.splitlines():
        s = line.strip()
        if not s or s.startswith("//"):
            continue
        lines.append(s)
    if not lines:
        return True
    for line in lines:
        if not (line.startswith("this.") and "=" in line and line.endswith(";")):
            return False
    return True

def remove_simple_constructors(content: str) -> tuple[str, bool]:
    if not any(s in content for s in SPRING_STEREOTYPES):
        return content, False
    modified = False
    pattern = re.compile(
        r"\n[ \t]*public\s+\w+\s*\([^)]*\)\s*\{",
        re.M,
    )
    while True:
        m = pattern.search(content)
        if not m:
            break
        start = m.start()
        j = m.end() - 1
        depth = 0
        k = j
        while k < len(content):
            if content[k] == "{":
                depth += 1
            elif content[k] == "}":
                depth -= 1
                if depth == 0:
                    body = content[j + 1 : k]
                    if constructor_only_assigns(body):
                        content = content[:start] + "\n" + content[k + 1 :]
                        modified = True
                    break
            k += 1
        else:
            break
        if not modified:
            break
        # only remove one constructor per iteration; restart
        modified_flag = True
        break
    # loop until no more - rewrite
    overall = False
    while True:
        m = pattern.search(content)
        if not m:
            break
        start = m.start()
        j = m.end() - 1
        depth = 0
        k = j
        end_k = None
        while k < len(content):
            if content[k] == "{":
                depth += 1
            elif content[k] == "}":
                depth -= 1
                if depth == 0:
                    end_k = k
                    break
            k += 1
        if end_k is None:
            break
        body = content[j + 1 : end_k]
        if constructor_only_assigns(body):
            content = content[:start] + "\n" + content[end_k + 1 :]
            overall = True
        else:
            # skip this constructor
            pattern = re.compile(
                r"\n[ \t]*public\s+\w+\s*\([^)]*\)\s*\{",
                re.M,
            )
            # advance past this match manually
            rest = content[m.end():]
            m2 = pattern.search(rest)
            if not m2:
                break
            # hack: replace first match with placeholder - simpler approach use finditer
            break
    # simpler loop with finditer and list
    return remove_constructors_iter(content)

def remove_constructors_iter(content: str) -> tuple[str, bool]:
    if not any(s in content for s in SPRING_STEREOTYPES):
        return content, False
    modified = False
    while True:
        found = False
        for m in re.finditer(r"\n([ \t]*)public\s+(\w+)\s*\(([^)]*)\)\s*\{", content):
            start = m.start()
            j = m.end() - 1
            depth = 0
            k = j
            end_k = None
            while k < len(content):
                if content[k] == "{":
                    depth += 1
                elif content[k] == "}":
                    depth -= 1
                    if depth == 0:
                        end_k = k
                        break
                k += 1
            if end_k is None:
                continue
            body = content[j + 1 : end_k]
            class_name = m.group(2)
            # must match enclosing class name
            before = content[:start]
            if not re.search(rf"public\s+class\s+{re.escape(class_name)}\s*\{{", before):
                continue
            if constructor_only_assigns(body):
                content = content[:start] + "\n" + content[end_k + 1 :]
                modified = True
                found = True
                break
        if not found:
            break
    return content, modified

def add_required_args_constructor(content: str) -> tuple[str, bool]:
    if not any(s in content for s in SPRING_STEREOTYPES):
        return content, False
    if "@RequiredArgsConstructor" in content:
        return content, False
    # need final dependency fields
    if not re.search(r"\n[ \t]*private\s+final\s+", content):
        return content, False
    if re.search(r"\n[ \t]*public\s+\w+\s*\(", content):
        return content, False  # still has constructor
    new = re.sub(
        r"(\n)(@(Service|Component|RestController|Repository|Controller)\s*\n)",
        r"\1@RequiredArgsConstructor\n\2",
        content,
        count=1,
    )
    if new == content:
        new = re.sub(
            r"(\n)(public\s+class\s+)",
            r"\1@RequiredArgsConstructor\n\2",
            content,
            count=1,
        )
    return new, "@RequiredArgsConstructor" in new and "@RequiredArgsConstructor" not in content

def add_data_to_class_block(block: str, extra_anns: list[str]) -> str:
    if "@Data" in block:
        return block
    anns = "\n".join("@" + a for a in extra_anns + ["Data"])
    # before public class / public static class
    return re.sub(
        r"(\n[ \t]*)(public\s+(?:static\s+)?class\s+)",
        r"\1" + anns + r"\n\1\2",
        block,
        count=1,
    )

def add_data_annotations(content: str, path: Path) -> tuple[str, bool]:
    reason = skip_reason(content, path)
    if reason:
        return content, False
    if "LoggerFactory" in content and path.name.endswith("LlmStartupLogger.java"):
        pass
    modified = False
    anns_needed = set()

    # entity package
    is_entity = "\\entity\\" in str(path) or "/entity/" in str(path).replace("\\", "/")

    # Process each class declaration (not record, not interface)
    def repl_class(m):
        nonlocal modified
        prefix = m.group(1)
        static = m.group(2) or ""
        cname = m.group(3)
        extra = []
        if is_entity and static == "" and cname.endswith("Entity"):
            extra.append("NoArgsConstructor")
        block_start = m.start()
        # only add if class has private fields and no remaining trivial issue
        modified = True
        anns = "\n".join(prefix + "@" + a for a in extra + ["Data"])
        return f"{prefix}{anns}\n{prefix}public {static}class {cname}"

    new_content = content
    # Don't add to nested records - class pattern excludes record
    class_pattern = re.compile(
        r"(\n[ \t]*)(public\s+(static\s+)?class\s+)(\w+)",
    )
    if class_pattern.search(content):
        # Check if file has fields worth @Data (private fields)
        if re.search(r"\n[ \t]*private\s+", content):
            new2 = class_pattern.sub(repl_class, content)
            if new2 != content:
                new_content = new2
                anns_needed.add("Data")
                if is_entity:
                    anns_needed.add("NoArgsConstructor")

    # ConfigurationProperties outer
    if "@ConfigurationProperties" in content and "@Data" not in content:
        new_content = re.sub(
            r"(\n@ConfigurationProperties[^\n]*\n)(public class)",
            r"\1@Data\n\2",
            new_content,
        )
        anns_needed.add("Data")

    return new_content, new_content != content or bool(anns_needed)

def process_file(path: Path) -> None:
    content = read_text(path)
    original = content
    reason = skip_reason(content, path)
    file_skipped = reason

    anns = set()
    if reason:
        skipped.append((str(path.relative_to(ROOT.parent.parent.parent.parent)), reason))
        # still apply slf4j/requiredargs on interfaces? skip interfaces entirely
        if reason == "interface":
            return
        if reason in ("enum", "exception", "top-level record"):
            # slf4j on LlmStartupLogger? it's component
            pass

    content, acc_mod = remove_trivial_accessors(content)

    if not file_skipped:
        content, data_mod = add_data_annotations(content, path)
        if data_mod:
            anns.add("Data")
            if "\\entity\\" in str(path):
                anns.add("NoArgsConstructor")

    content, slf4j_mod = apply_slf4j(content)
    if slf4j_mod:
        anns.add("Slf4j")

    content, cons_mod = remove_constructors_iter(content)
    content, rac_mod = add_required_args_constructor(content)
    if rac_mod:
        anns.add("RequiredArgsConstructor")

    content = ensure_imports(content, anns)

    # clean double blank lines
    content = re.sub(r"\n{3,}", "\n\n", content)

    if content != original:
        write_text(path, content)
        changed.append(str(path.relative_to(ROOT.parent.parent.parent.parent)))

# walk
for p in sorted(ROOT.rglob("*.java")):
    process_file(p)

print(f"Changed: {len(changed)}")
for c in changed:
    print(c)
print(f"Skipped categories: {len(skipped)}")
from collections import Counter
for k, v in Counter(r for _, r in skipped).items():
    print(f"  {k}: {v}")
