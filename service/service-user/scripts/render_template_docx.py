#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Render a DOCX template by filling ${placeholder_key} values.

Usage:
    python3 render_template_docx.py <template.docx> <fields.json> <output.docx>

The renderer favours stable output over clever layout surgery:
- paragraphs, tables, headers and footers are visited;
- ${field_key} placeholders are replaced with Dify extracted values;
- null values become blank strings;
- multiline values are kept as Word line breaks.
"""
import json
import re
import sys
from pathlib import Path

from docx import Document


PLACEHOLDER_RE = re.compile(r"\$\{([A-Za-z0-9_]+)\}")


def load_fields(path: Path):
    with path.open("r", encoding="utf-8") as file:
        raw = json.load(file)
    fields = {}
    for key, value in (raw or {}).items():
        if value is None:
            fields[str(key)] = ""
        else:
            fields[str(key)] = str(value)
    return fields


def replace_text(text: str, fields: dict):
    def repl(match):
        key = match.group(1)
        return fields.get(key, match.group(0))

    return PLACEHOLDER_RE.sub(repl, text or "")


def set_run_text_with_breaks(run, value: str):
    parts = value.splitlines()
    if not parts:
        run.text = ""
        return
    run.text = parts[0]
    for part in parts[1:]:
        run.add_break()
        run.add_text(part)


def clear_paragraph_to_text(paragraph, value: str):
    runs = paragraph.runs
    if not runs:
        set_run_text_with_breaks(paragraph.add_run(), value)
        return
    set_run_text_with_breaks(runs[0], value)
    for index in range(len(runs) - 1, 0, -1):
        paragraph._p.remove(runs[index]._r)


def render_paragraph(paragraph, fields: dict):
    original = paragraph.text or ""
    if "${" not in original:
        return
    rendered = replace_text(original, fields)
    if rendered != original:
        clear_paragraph_to_text(paragraph, rendered)


def render_table(table, fields: dict):
    for row in table.rows:
        for cell in row.cells:
            render_block(cell, fields)


def render_block(block, fields: dict):
    for paragraph in block.paragraphs:
        render_paragraph(paragraph, fields)
    for table in block.tables:
        render_table(table, fields)


def render_document(template_path: Path, fields_path: Path, output_path: Path):
    fields = load_fields(fields_path)
    document = Document(str(template_path))
    render_block(document, fields)
    for section in document.sections:
        render_block(section.header, fields)
        render_block(section.footer, fields)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    document.save(str(output_path))


def main():
    if len(sys.argv) != 4:
        print(__doc__)
        return 2
    render_document(Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
