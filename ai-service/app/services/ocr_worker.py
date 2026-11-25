"""Isolated OCR process: PDF page on stdin, UTF-8 text on stdout."""
import math
import sys

import pymupdf


MAX_PIXELS = 12_000_000
DPI = 250


def recognize(page_pdf: bytes, tessdata: str) -> str:
    with pymupdf.open(stream=page_pdf, filetype="pdf") as document:
        page = document[0]
        area = page.rect.width * page.rect.height
        if not math.isfinite(area) or area <= 0:
            raise ValueError("Invalid page dimensions")
        scale = min(DPI / 72, math.sqrt(MAX_PIXELS / area))
        pixmap = page.get_pixmap(
            matrix=pymupdf.Matrix(scale, scale),
            colorspace=pymupdf.csRGB,
            alpha=False,
        )
        pixmap.set_dpi(round(72 * scale), round(72 * scale))
        searchable_pdf = pixmap.pdfocr_tobytes(language="rus+eng", tessdata=tessdata)
        with pymupdf.open(stream=searchable_pdf, filetype="pdf") as recognized:
            return recognized[0].get_text("text").strip()


if __name__ == "__main__":
    try:
        text = recognize(sys.stdin.buffer.read(), sys.argv[1])
        sys.stdout.buffer.write(text.encode("utf-8"))
    except Exception as exception:
        # Only the exception class is reported; document contents stay private.
        sys.stderr.write(type(exception).__name__)
        sys.exit(1)
