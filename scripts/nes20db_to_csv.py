#!/usr/bin/env python3
"""Convert NewRisingSun's nes20db.xml into a flat CSV file.

Usage:
    python nes20db_to_csv.py nes20db.xml
    python nes20db_to_csv.py nes20db.xml nes20db.csv

Each direct child element of a <game> entry becomes CSV columns named
"<tag>_<attribute>", for example: rom_sha1, console_region, pcb_mapper.
Missing optional XML elements are written as empty cells.
"""

from __future__ import annotations

import argparse
import csv
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterable

REGION_NAMES = {
    "0": "NTSC",
    "1": "PAL",
    "2": "MULTI_REGION",
    "3": "DENDY",
}

PREFERRED_COLUMNS = [
    "rom_sha1",
    "console_region",
    "pcb_mapper",
    "pcb_submapper",
    "pcb_mirroring",
]


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def entry_to_row(entry: ET.Element) -> dict[str, str]:
    row: dict[str, str] = {}
    duplicate_counts: dict[str, int] = {}

    for attribute, value in entry.attrib.items():
        row[f"entry_{local_name(attribute)}"] = value

    for child in entry:
        tag = local_name(child.tag)

        count = duplicate_counts.get(tag, 0) + 1
        duplicate_counts[tag] = count
        prefix = tag if count == 1 else f"{tag}_{count}"

        for attribute, value in child.attrib.items():
            row[f"{prefix}_{local_name(attribute)}"] = value

        text = (child.text or "").strip()
        if text:
            row[f"{prefix}_text"] = text

    region = row.get("console_region")
    if region is not None:
        row["console_region_name"] = REGION_NAMES.get(region, "UNKNOWN")

    return row


def iter_entries(root: ET.Element) -> Iterable[ET.Element]:
    children = list(root)
    if not children:
        return

    yield from children


def ordered_columns(rows: list[dict[str, str]]) -> list[str]:
    discovered = {column for row in rows for column in row}
    return [column for column in PREFERRED_COLUMNS if column in discovered]


def convert(input_path: Path, output_path: Path) -> int:
    try:
        root = ET.parse(input_path).getroot()
    except ET.ParseError as error:
        raise ValueError(f"Invalid XML in {input_path}: {error}") from error

    rows = [entry_to_row(entry) for entry in iter_entries(root)]
    if not rows:
        raise ValueError(f"No database entries found in {input_path}")

    columns = ordered_columns(rows)

    with output_path.open("w", encoding="utf-8", newline="") as output_file:
        writer = csv.DictWriter(
            output_file,
            fieldnames=columns,
            extrasaction="ignore",
            lineterminator="\n",
        )
        writer.writeheader()
        writer.writerows(rows)

    return len(rows)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert nes20db.xml into a flat UTF-8 CSV file."
    )
    parser.add_argument("input", type=Path, help="Path to nes20db.xml")
    parser.add_argument(
        "output",
        nargs="?",
        type=Path,
        help="Output CSV path; defaults to the input filename with .csv",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    input_path: Path = args.input
    output_path: Path = args.output or input_path.with_suffix(".csv")

    if not input_path.is_file():
        print(f"Error: input file does not exist: {input_path}", file=sys.stderr)
        return 1

    try:
        count = convert(input_path, output_path)
    except (OSError, ValueError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1

    print(f"Wrote {count} entries to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
