#!/usr/bin/env python3
"""Extract old counter and new stable IDs from generated JVM class files."""

import argparse
import csv
import os
import re
import struct
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

# Ids embedded in a generated JVM name, in either of the two forms a symbol can carry.
#
#   counter  $229042           a GenSym sequence number; unstable across builds
#   stable   $0ok7b0wx9im0g    13 base-36 digits of a content hash; reproducible
#
# Both are matched, because the point of the census is to watch one replace the other.
# Matching only counters would report success the moment they disappear, even if nothing
# replaced them; matching only hashes would hide any counter that survives.
#
# The stable form is tried first: it is exactly 13 characters, so an all-digit hash would
# otherwise be misread as a counter. Counter IDs may have any number of decimal digits.
STABLE_WIDTH = 13
STABLE_ID_PATTERN = re.compile(r"\$([0-9a-z]{%d})(?![0-9a-z])" % STABLE_WIDTH)
COUNTER_ID_PATTERN = re.compile(r"\$(\d+)(?![0-9a-z])")


def find_ids(text: str) -> List[str]:
    """Return every id embedded in `text`, of either form."""
    if not text:
        return []
    stable = STABLE_ID_PATTERN.findall(text)
    counters = [c for c in COUNTER_ID_PATTERN.findall(text) if len(c) != STABLE_WIDTH]
    return stable + counters


def is_counter(value: str) -> bool:
    """Return True if `value` is a GenSym counter rather than a content hash."""
    return value.isdigit() and len(value) != STABLE_WIDTH


# ==============================================================================
# Bytecode & Symbol Extraction
# ==============================================================================

def decode_type_descriptor(desc: str) -> str:
    """Convert JVM bytecode type descriptor into human-readable type."""
    type_map = {
        "B": "byte", "C": "char", "D": "double", "F": "float",
        "I": "int", "J": "long", "S": "short", "Z": "boolean", "V": "void",
    }
    if not desc:
        return ""
    if desc.startswith("["):
        return decode_type_descriptor(desc[1:]) + "[]"
    if desc.startswith("L") and desc.endswith(";"):
        return desc[1:-1].replace("/", ".")
    return type_map.get(desc, desc)


def decode_method_descriptor(desc: str) -> Tuple[List[str], str]:
    """Parse method descriptor like (Ljava/lang/String;I)V into ([params], return_type)."""
    if not desc or not desc.startswith("("):
        return [], desc

    end_params = desc.find(")")
    if end_params == -1:
        return [], desc

    param_str = desc[1:end_params]
    ret_str = desc[end_params + 1:]

    params = []
    i = 0
    while i < len(param_str):
        c = param_str[i]
        if c in "BCDFIJSZ":
            params.append(decode_type_descriptor(c))
            i += 1
        elif c == "[":
            array_depth = 0
            while i < len(param_str) and param_str[i] == "[":
                array_depth += 1
                i += 1
            if i < len(param_str):
                if param_str[i] == "L":
                    semi = param_str.find(";", i)
                    elem = param_str[i + 1:semi].replace("/", ".") if semi != -1 else param_str[i:]
                    i = semi + 1 if semi != -1 else len(param_str)
                else:
                    elem = decode_type_descriptor(param_str[i])
                    i += 1
                params.append(elem + "[]" * array_depth)
        elif c == "L":
            semi = param_str.find(";", i)
            elem = param_str[i + 1:semi].replace("/", ".") if semi != -1 else param_str[i + 1:]
            params.append(elem)
            i = semi + 1 if semi != -1 else len(param_str)
        else:
            i += 1

    return params, decode_type_descriptor(ret_str)


class ClassFileParser:
    """Direct JVM .class file binary parser."""

    def __init__(self, data: bytes):
        self.data = data
        self.offset = 0
        self.cp: List[Any] = []

    def parse(self) -> Optional[Dict[str, Any]]:
        if len(self.data) < 10:
            return None

        magic, _, _, cp_count = struct.unpack_from(">IHHH", self.data, 0)
        if magic != 0xCAFEBABE:
            return None

        self.offset = 10
        self.cp = [None] * cp_count
        i = 1
        while i < cp_count:
            if self.offset >= len(self.data):
                break
            tag = self.data[self.offset]
            self.offset += 1
            if tag == 1:  # Utf8
                length = struct.unpack_from(">H", self.data, self.offset)[0]
                self.offset += 2
                val = self.data[self.offset:self.offset + length].decode("utf-8", errors="replace")
                self.offset += length
                self.cp[i] = ("Utf8", val)
            elif tag in (3, 4):  # Integer, Float
                val = struct.unpack_from(">I", self.data, self.offset)[0]
                self.offset += 4
                self.cp[i] = ("IntFloat", val)
            elif tag in (5, 6):  # Long, Double
                val = struct.unpack_from(">Q", self.data, self.offset)[0]
                self.offset += 8
                self.cp[i] = ("LongDouble", val)
                i += 1
            elif tag == 7:  # Class
                name_idx = struct.unpack_from(">H", self.data, self.offset)[0]
                self.offset += 2
                self.cp[i] = ("Class", name_idx)
            elif tag == 8:  # String
                str_idx = struct.unpack_from(">H", self.data, self.offset)[0]
                self.offset += 2
                self.cp[i] = ("String", str_idx)
            elif tag in (9, 10, 11):  # Fieldref, Methodref, InterfaceMethodref
                cls_idx, nt_idx = struct.unpack_from(">HH", self.data, self.offset)
                self.offset += 4
                self.cp[i] = ("Ref", cls_idx, nt_idx)
            elif tag == 12:  # NameAndType
                name_idx, desc_idx = struct.unpack_from(">HH", self.data, self.offset)
                self.offset += 4
                self.cp[i] = ("NameAndType", name_idx, desc_idx)
            elif tag == 15:  # MethodHandle
                self.offset += 3
                self.cp[i] = ("MethodHandle",)
            elif tag == 16:  # MethodType
                desc_idx = struct.unpack_from(">H", self.data, self.offset)[0]
                self.offset += 2
                self.cp[i] = ("MethodType", desc_idx)
            elif tag in (17, 18):  # Dynamic, InvokeDynamic
                self.offset += 4
                self.cp[i] = ("Dynamic",)
            elif tag in (19, 20):  # Module, Package
                self.offset += 2
                self.cp[i] = ("ModulePackage",)
            else:
                break
            i += 1

        if self.offset + 8 > len(self.data):
            return None

        _, this_class_idx, super_class_idx, interfaces_count = struct.unpack_from(
            ">HHHH", self.data, self.offset
        )
        self.offset += 8

        this_class = self.get_class_name(this_class_idx)
        super_class = self.get_class_name(super_class_idx)

        interfaces = []
        for _ in range(interfaces_count):
            if self.offset + 2 > len(self.data):
                break
            iface_idx = struct.unpack_from(">H", self.data, self.offset)[0]
            self.offset += 2
            interfaces.append(self.get_class_name(iface_idx))

        if self.offset + 2 > len(self.data):
            return None
        fields_count = struct.unpack_from(">H", self.data, self.offset)[0]
        self.offset += 2

        fields = []
        for _ in range(fields_count):
            if self.offset + 8 > len(self.data):
                break
            _, f_name_idx, f_desc_idx, f_attr_count = struct.unpack_from(">HHHH", self.data, self.offset)
            self.offset += 8
            name = self.get_utf8(f_name_idx)
            desc = self.get_utf8(f_desc_idx)
            fields.append({"name": name, "descriptor": desc, "type": decode_type_descriptor(desc)})
            for _ in range(f_attr_count):
                if self.offset + 6 > len(self.data):
                    break
                _, attr_len = struct.unpack_from(">HI", self.data, self.offset)
                self.offset += 6 + attr_len

        if self.offset + 2 > len(self.data):
            return None
        methods_count = struct.unpack_from(">H", self.data, self.offset)[0]
        self.offset += 2

        methods = []
        for _ in range(methods_count):
            if self.offset + 8 > len(self.data):
                break
            _, m_name_idx, m_desc_idx, m_attr_count = struct.unpack_from(">HHHH", self.data, self.offset)
            self.offset += 8
            name = self.get_utf8(m_name_idx)
            desc = self.get_utf8(m_desc_idx)
            params, ret = decode_method_descriptor(desc)
            methods.append({
                "name": name,
                "descriptor": desc,
                "parameters": params,
                "return_type": ret,
                "signature": f"{name}({', '.join(params)}): {ret}",
            })
            for _ in range(m_attr_count):
                if self.offset + 6 > len(self.data):
                    break
                _, attr_len = struct.unpack_from(">HI", self.data, self.offset)
                self.offset += 6 + attr_len

        cp_strings = [
            entry[1] for entry in self.cp if entry and entry[0] == "Utf8" and entry[1]
        ]

        return {
            "class": this_class.replace("/", "."),
            "super": super_class.replace("/", ".") if super_class else "",
            "interfaces": [iface.replace("/", ".") for iface in interfaces if iface],
            "fields": fields,
            "methods": methods,
            "constant_pool_strings": cp_strings,
        }

    def get_utf8(self, idx: int) -> str:
        if 0 < idx < len(self.cp) and self.cp[idx] and self.cp[idx][0] == "Utf8":
            return self.cp[idx][1]
        return ""

    def get_class_name(self, idx: int) -> str:
        if 0 < idx < len(self.cp) and self.cp[idx] and self.cp[idx][0] == "Class":
            return self.get_utf8(self.cp[idx][1])
        return ""


def extract_symbols_and_ids(class_dir: Path) -> Tuple[List[Dict[str, str]], Set[str], int]:
    """Extract symbol rows and target ID set from all class files in class_dir."""
    rows: List[Dict[str, str]] = []
    target_ids: Set[str] = set()
    classes_count = 0

    for root, _, files in os.walk(class_dir):
        for file in sorted(files):
            if not file.endswith(".class"):
                continue
            full_path = Path(root) / file
            rel_path = full_path.relative_to(class_dir)
            try:
                data = full_path.read_bytes()
                parser = ClassFileParser(data)
                info = parser.parse()
                if not info:
                    continue
                classes_count += 1
                cls_name = info["class"]

                # IDs from class name
                for m in find_ids(cls_name):
                    target_ids.add(m)

                class_id = ",".join(find_ids(cls_name))
                rows.append({
                    "kind": "class",
                    "class": cls_name,
                    "symbol": cls_name,
                    "id": class_id,
                    "descriptor": "",
                    "signature": f"class {cls_name}" + (f" extends {info['super']}" if info.get("super") else ""),
                    "file": str(rel_path),
                })

                # Fields
                for f in info.get("fields", []):
                    sym_name = f"{cls_name}.{f['name']}"
                    for m in find_ids(sym_name):
                        target_ids.add(m)
                    f_id = ",".join(find_ids(sym_name))
                    rows.append({
                        "kind": "field",
                        "class": cls_name,
                        "symbol": sym_name,
                        "id": f_id,
                        "descriptor": f["descriptor"],
                        "signature": f"{f['type']} {f['name']}",
                        "file": str(rel_path),
                    })

                # Methods
                for m in info.get("methods", []):
                    sym_name = f"{cls_name}::{m['name']}"
                    for match_id in find_ids(sym_name):
                        target_ids.add(match_id)
                    m_id = ",".join(find_ids(sym_name))
                    rows.append({
                        "kind": "method",
                        "class": cls_name,
                        "symbol": sym_name,
                        "id": m_id,
                        "descriptor": m["descriptor"],
                        "signature": m["signature"],
                        "file": str(rel_path),
                    })

                # Constant pool strings
                for s in info.get("constant_pool_strings", []):
                    for match_id in find_ids(s):
                        target_ids.add(match_id)

            except Exception as e:
                sys.stderr.write(f"Warning: failed to parse {full_path}: {e}\n")

    return rows, target_ids, classes_count


# ==============================================================================
# Trace File Updating & Marking
# ==============================================================================

def mark_fresh_ids_trace(trace_path: Path, target_ids: Set[str], match_on: Set[str]) -> Dict[str, Any]:
    """Add marked column (true/false) to fresh-ids CSV in-place."""
    stats: Dict[str, Any] = {
        "total_rows": 0,
        "marked_rows": 0,
        "id_matches": 0,
        "oid_matches": 0,
        "by_phase": {},
    }

    temp_path = trace_path.with_suffix(".tmp")
    with open(trace_path, "r", encoding="utf-8", newline="") as in_f, \
         open(temp_path, "w", encoding="utf-8", newline="") as out_f:

        reader = csv.DictReader(in_f)
        if not reader.fieldnames:
            raise ValueError(f"Empty or invalid CSV file: {trace_path}")

        fieldnames = [f for f in reader.fieldnames if f != "marked"] + ["marked"]
        writer = csv.DictWriter(out_f, fieldnames=fieldnames)
        writer.writeheader()

        for row in reader:
            stats["total_rows"] += 1
            row_id = (row.get("id") or "").strip()
            row_oid = (row.get("oid") or "").strip()

            m_id = ("id" in match_on) and (row_id in target_ids) and bool(row_id)
            m_oid = ("oid" in match_on) and (row_oid in target_ids) and bool(row_oid)

            m_owner = False
            if "owner" in match_on:
                for oid in find_ids(row.get("owner") or ""):
                    if oid in target_ids:
                        m_owner = True
                        break

            m_name = False
            if "name" in match_on:
                for nid in find_ids(row.get("name") or ""):
                    if nid in target_ids:
                        m_name = True
                        break

            is_matched = bool(m_id or m_oid or m_owner or m_name)
            if is_matched:
                stats["marked_rows"] += 1
                if m_id:
                    stats["id_matches"] += 1
                if m_oid:
                    stats["oid_matches"] += 1
                phase = row.get("phase") or "unknown"
                stats["by_phase"][phase] = stats["by_phase"].get(phase, 0) + 1
                row["marked"] = "true"
            else:
                row["marked"] = "false"

            writer.writerow(row)

    temp_path.replace(trace_path)
    return stats


# Trace File & Output Helpers
# ==============================================================================

def find_latest_trace(project_dir: Path) -> Optional[Path]:
    """Find the most recent fresh-ids-*.csv file."""
    candidates = list(project_dir.glob("fresh-ids-*.csv"))
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


# ==============================================================================
# Main Pipeline
# ==============================================================================

def main():
    parser = argparse.ArgumentParser(
        description="Extract old counter and new stable IDs from generated class files.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument(
        "class_dir",
        nargs="?",
        default="build/class",
        help="Generated class directory to scan",
    )
    parser.add_argument(
        "-o", "--output-dir",
        default=".",
        help="Directory for generated-class-symbols.csv and generated-class-ids.txt",
    )
    args = parser.parse_args()

    class_dir = Path(args.class_dir).resolve()
    output_dir = Path(args.output_dir).resolve()
    if not class_dir.exists():
        sys.stderr.write(f"Error: Class directory not found: {class_dir}\n")
        sys.exit(1)
    output_dir.mkdir(parents=True, exist_ok=True)

    print(f">> Extracting symbols from {class_dir}...")
    symbol_rows, target_ids, classes_count = extract_symbols_and_ids(class_dir)

    symbols_csv = output_dir / "generated-class-symbols.csv"
    with open(symbols_csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["kind", "class", "symbol", "id", "descriptor", "signature", "file"])
        writer.writeheader()
        for r in symbol_rows:
            writer.writerow(r)

    ids_txt = output_dir / "generated-class-ids.txt"
    with open(ids_txt, "w", encoding="utf-8") as f:
        # Counters first, in numeric order, then content hashes lexicographically. The key
        # has to be uniform: the two forms are not comparable to each other.
        def order(tid: str) -> Tuple[int, int, str]:
            return (0, int(tid), "") if is_counter(tid) else (1, 0, tid)

        for tid in sorted(target_ids, key=order):
            f.write(f"{tid}\n")

    print("\n" + "=" * 60)
    print(" SUMMARY")
    print("=" * 60)
    print(f" - Bytecode classes scanned:  {classes_count:,}")
    print(f" - Total symbols extracted:  {len(symbol_rows):,}")
    # Split the census by form. Counters must fall to zero and stable ids must rise to
    # replace them; reporting only the total would hide either half going wrong.
    counters = {i for i in target_ids if is_counter(i)}
    stable = target_ids - counters
    print(f" - Generated IDs found:      {len(target_ids):,}")
    print(f"   * Counter-derived:        {len(counters):,}")
    print(f"   * Content-addressed:      {len(stable):,}")
    print(f" - Saved symbols CSV:        {symbols_csv}")
    print(f" - Saved IDs list:           {ids_txt}")
    print("=" * 60 + "\n")


if __name__ == "__main__":
    main()
