#!/usr/bin/env python3
"""Convert a PDF file to DOCX when pdf2docx is available."""
import sys
from pathlib import Path


def main():
    if len(sys.argv) != 3:
        print("usage: convert_pdf_to_docx.py <input.pdf> <output.docx>", file=sys.stderr)
        return 2

    input_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2])
    try:
        from pdf2docx import Converter
    except Exception as exc:
        print(f"pdf2docx is not installed: {exc}", file=sys.stderr)
        return 3

    output_path.parent.mkdir(parents=True, exist_ok=True)
    converter = Converter(str(input_path))
    try:
        converter.convert(str(output_path), start=0, end=None)
    finally:
        converter.close()

    if not output_path.exists():
        print("conversion finished but output file was not created", file=sys.stderr)
        return 4
    print(str(output_path))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
