import logging
import os
from pathlib import Path
import subprocess
import sys
import time

import pymupdf


LOGGER = logging.getLogger(__name__)
DEFAULT_TESSDATA = Path(__file__).resolve().parents[2] / "models" / "tessdata"
MAX_OCR_PAGES = 20
PAGE_TIMEOUT_SECONDS = 20
DOCUMENT_TIMEOUT_SECONDS = 45


class PdfTextExtractor:

    def __init__(self, tessdata: str | Path | None = None) -> None:
        self.tessdata = Path(tessdata or os.getenv("TESSDATA_PREFIX") or DEFAULT_TESSDATA)

    def _recognize_page(self, document, page_number: int, timeout: float) -> str:
        if not all((self.tessdata / f"{lang}.traineddata").is_file() for lang in ("rus", "eng")):
            raise RuntimeError("OCR language data unavailable; see ai-service/README.md")
        with pymupdf.open() as single_page:
            single_page.insert_pdf(document, from_page=page_number, to_page=page_number)
            payload = single_page.tobytes()
        process = subprocess.run(
            [sys.executable, "-B", str(Path(__file__).with_name("ocr_worker.py")),
             "."],
            # Tesseract's Windows file access cannot handle Cyrillic model paths.
            # The OS can set cwd to that directory, so use a relative data path.
            cwd=self.tessdata.resolve(),
            input=payload,
            capture_output=True,
            timeout=timeout,
            check=True,
            creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0,
        )
        return process.stdout.decode("utf-8").strip()

    @staticmethod
    def _needs_ocr(page, text: str) -> bool:
        images = page.get_image_info()
        if not text:
            return bool(images or page.get_drawings())
        page_area = page.rect.get_area()
        return any(
            ((pymupdf.Rect(info["bbox"]) * page.rotation_matrix) & page.rect).get_area()
            >= 0.20 * page_area
            for info in images
        )

    def extract(self, file_bytes: bytes) -> dict:
        try:
            document = pymupdf.open(
                stream=file_bytes,
                filetype="pdf",
            )
        except Exception as exception:
            raise ValueError(
                "Не удалось открыть PDF-файл"
            ) from exception

        try:
            pages_text: list[str] = []
            ocr_used = False
            requires_ocr = False
            ocr_attempts = 0
            started = time.monotonic()

            for page in document:
                page_text = page.get_text("text").strip()

                if self._needs_ocr(page, page_text):
                    remaining = DOCUMENT_TIMEOUT_SECONDS - (time.monotonic() - started)
                    if ocr_attempts >= MAX_OCR_PAGES or remaining <= 0:
                        requires_ocr = True
                    else:
                        ocr_attempts += 1
                        try:
                            recognized = self._recognize_page(
                                document, page.number, min(PAGE_TIMEOUT_SECONDS, remaining)
                            )
                            ocr_used = True
                            if recognized:
                                # Native text contains exact identifiers which raster OCR
                                # may miss. Keep it and append only new recognized lines.
                                native_lines = {" ".join(line.casefold().split())
                                                for line in page_text.splitlines()}
                                additional = [line for line in recognized.splitlines()
                                              if " ".join(line.casefold().split()) not in native_lines]
                                page_text = "\n".join(filter(None, [page_text, "\n".join(additional)]))
                            else:
                                requires_ocr = True
                        except (RuntimeError, OSError, subprocess.SubprocessError, UnicodeError):
                            LOGGER.warning("OCR incomplete on page %s", page.number + 1)
                            requires_ocr = True

                if page_text:
                    pages_text.append(page_text)

            text = "\n\n".join(pages_text).strip()

            return {
                "page_count": document.page_count,
                "text": text,
                "character_count": len(text),
                "has_text": bool(text),
                "ocr_used": ocr_used,
                "requires_ocr": requires_ocr or not bool(text),
            }

        finally:
            document.close()
