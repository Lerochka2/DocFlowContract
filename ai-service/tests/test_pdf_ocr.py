import unittest
from pathlib import Path
import subprocess
import tempfile
import asyncio
from io import BytesIO
import time
from unittest.mock import patch

import pymupdf

from app.services.pdf_text_extractor import PdfTextExtractor


CONTRACT = (
    "ДОГОВОР ПОСТАВКИ № 125\n"
    "Поставщик обязуется передать товар,\n"
    "а покупатель принять и оплатить его.\n"
    "Стоимость договора составляет 50000 рублей.\n"
    "Срок поставки товара: 30 дней.\n"
    "Права и обязанности сторон определены договором."
)


def make_pdf(*pages):
    """Create text, image-only and mixed pages without storing customer data."""
    with pymupdf.open() as document:
        for kind in pages:
            with pymupdf.open() as source:
                original = source.new_page()
                font = next((p for p in (
                    Path("C:/Windows/Fonts/arial.ttf"),
                    Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                ) if p.is_file()), None)
                if font is None:
                    raise unittest.SkipTest("A Cyrillic font is required for the OCR fixture")
                original.insert_font(fontname="cyr", fontfile=str(font))
                original.insert_text((50, 100), CONTRACT, fontname="cyr", fontsize=18)
                page = document.new_page()
                if kind == "text":
                    page.show_pdf_page(page.rect, source, 0)
                elif kind in ("scan", "scan-with-footer"):
                    pixmap = original.get_pixmap(matrix=pymupdf.Matrix(2, 2))
                    page.insert_image(page.rect, stream=pixmap.tobytes("png"))
                    if kind == "scan-with-footer":
                        page.insert_text((50, 810), "Page 1")
        return document.tobytes()


class PdfOcrTest(unittest.TestCase):
    def test_native_text_is_preserved_without_ocr(self):
        result = PdfTextExtractor().extract(make_pdf("text"))
        self.assertIn("ДОГОВОР ПОСТАВКИ", " ".join(result["text"].split()))
        self.assertFalse(result.get("ocr_used", False))

    def test_recognizes_russian_image_only_pdf(self):
        pdf = make_pdf("scan")
        with pymupdf.open(stream=pdf, filetype="pdf") as source:
            self.assertEqual("", source[0].get_text().strip())
        result = PdfTextExtractor().extract(pdf)
        self.assertIn("договор", result["text"].lower())
        self.assertIn("поставки", result["text"].lower())
        self.assertTrue(result["ocr_used"])
        self.assertFalse(result["requires_ocr"])

    def test_mixed_pdf_keeps_native_and_scanned_pages(self):
        result = PdfTextExtractor().extract(make_pdf("text", "scan"))
        self.assertEqual(2, result["page_count"])
        self.assertGreaterEqual(" ".join(result["text"].lower().split()).count("договор поставки"), 2)
        self.assertTrue(result["ocr_used"])

    def test_text_footer_does_not_hide_scan(self):
        result = PdfTextExtractor().extract(make_pdf("scan-with-footer"))
        self.assertIn("договор", result["text"].lower())
        self.assertTrue(result["ocr_used"])

    def test_invalid_pdf_has_existing_validation_error(self):
        with self.assertRaises(ValueError):
            PdfTextExtractor().extract(b"not a PDF")

    def test_missing_models_preserve_text_and_require_review(self):
        with tempfile.TemporaryDirectory() as missing:
            result = PdfTextExtractor(tessdata=missing).extract(make_pdf("text", "scan"))
        self.assertIn("ДОГОВОР", result["text"])
        self.assertTrue(result["requires_ocr"])
        self.assertFalse(result["ocr_used"])

    def test_timeout_preserves_native_text(self):
        with patch("app.services.pdf_text_extractor.subprocess.run",
                   side_effect=subprocess.TimeoutExpired("ocr_worker", 20)):
            result = PdfTextExtractor().extract(make_pdf("text", "scan"))
        self.assertIn("ДОГОВОР", result["text"])
        self.assertTrue(result["requires_ocr"])

    def test_page_limit_does_not_accept_partial_extraction(self):
        with patch("app.services.pdf_text_extractor.MAX_OCR_PAGES", 0):
            result = PdfTextExtractor().extract(make_pdf("text", "scan"))
        self.assertTrue(result["has_text"])
        self.assertTrue(result["requires_ocr"])

    def test_empty_page_does_not_crash(self):
        result = PdfTextExtractor().extract(make_pdf("blank"))
        self.assertFalse(result["has_text"])
        self.assertTrue(result["requires_ocr"])

    def test_api_classifies_real_scan(self):
        from app.main import analyze_pdf_content
        result = analyze_pdf_content("scan.pdf", make_pdf("scan"))
        self.assertTrue(result["hasText"])
        self.assertTrue(result["ocrUsed"])
        self.assertFalse(result["requiresOcr"])
        self.assertTrue(result["classificationAvailable"])
        self.assertEqual("CONTRACT", result["predictedType"])
        self.assertIsNotNone(result["confidence"])

    def test_api_forces_review_if_scan_is_unread(self):
        import app.main as api
        with tempfile.TemporaryDirectory() as missing, patch.object(
            api, "pdf_text_extractor", PdfTextExtractor(tessdata=missing)
        ):
            result = api.analyze_pdf_content("mixed.pdf", make_pdf("text", "scan"))
        self.assertTrue(result["requiresOcr"])
        self.assertTrue(result["reviewRequired"])
        self.assertFalse(result["classificationAccepted"])
        self.assertIsNone(result["documentType"])
        self.assertTrue(result["hasText"])

    def test_ocr_preserves_exact_native_identifier(self):
        with pymupdf.open(stream=make_pdf("scan"), filetype="pdf") as document:
            document[0].insert_text((40, 820), "NATIVE_UNIQUE_17823456", fontsize=1)
            result = PdfTextExtractor().extract(document.tobytes())
        self.assertIn("NATIVE_UNIQUE_17823456", result["text"])
        self.assertIn("договор", result["text"].lower())

    def test_rotated_mixed_page_still_requires_ocr(self):
        with pymupdf.open(stream=make_pdf("scan"), filetype="pdf") as source:
            image = source[0].get_images()[0][0]
            image_bytes = source.extract_image(image)["image"]
        with pymupdf.open() as document:
            page = document.new_page(width=600, height=840)
            page.insert_text((40, 40), "Native header")
            page.insert_image(pymupdf.Rect(0, 450, 600, 840), stream=image_bytes, keep_proportion=False)
            page.set_rotation(90)
            self.assertTrue(PdfTextExtractor._needs_ocr(page, page.get_text()))


class AsyncOcrTest(unittest.IsolatedAsyncioTestCase):
    async def test_ocr_does_not_block_other_async_requests(self):
        import app.main as api
        from fastapi import UploadFile
        from starlette.datastructures import Headers
        upload = UploadFile(BytesIO(make_pdf("scan")), filename="scan.pdf",
                            headers=Headers({"content-type": "application/pdf"}))

        slow_started = []

        def slow_ocr(*args):
            slow_started.append(time.monotonic())
            time.sleep(0.6)
            return CONTRACT

        async def heartbeat():
            while not slow_started:
                await asyncio.sleep(0.01)
            await asyncio.sleep(0.03)
            return time.monotonic() - slow_started[0]

        with patch.object(PdfTextExtractor, "_recognize_page", side_effect=slow_ocr):
            result, latency = await asyncio.gather(api.analyze_document(upload), heartbeat())
        self.assertTrue(result["ocrUsed"])
        self.assertLess(latency, 0.4, "OCR blocked the event loop")

    async def test_busy_service_rejects_extra_pdf_without_queueing(self):
        import app.main as api
        from fastapi import HTTPException
        from threading import Event
        started = Event()

        def work():
            started.set()
            time.sleep(0.2)
            return "done"

        first = asyncio.create_task(api.run_pdf_task(work))
        while not started.is_set():
            await asyncio.sleep(0.01)
        try:
            with self.assertRaises(HTTPException) as error:
                await api.run_pdf_task(lambda: "unexpected")
            self.assertEqual(503, error.exception.status_code)
        finally:
            self.assertEqual("done", await first)
        self.assertEqual("next", await api.run_pdf_task(lambda: "next"))

    async def test_cancelled_request_keeps_slot_until_work_finishes(self):
        import app.main as api
        from fastapi import HTTPException
        from threading import Event
        started, finished = Event(), Event()

        def work():
            started.set()
            time.sleep(0.2)
            finished.set()

        first = asyncio.create_task(api.run_pdf_task(work))
        while not started.is_set():
            await asyncio.sleep(0.01)
        first.cancel()
        with self.assertRaises(asyncio.CancelledError):
            await first
        with self.assertRaises(HTTPException):
            await api.run_pdf_task(lambda: "unexpected")
        while not finished.is_set():
            await asyncio.sleep(0.01)
        await asyncio.sleep(0.03)
        self.assertEqual("next", await api.run_pdf_task(lambda: "next"))


if __name__ == "__main__":
    unittest.main()
