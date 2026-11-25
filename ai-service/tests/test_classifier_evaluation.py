"""The PDF benchmark counts each distinct document only once."""

import csv
import hashlib
from pathlib import Path
import tempfile
import unittest

import pymupdf

from scripts.evaluate_classifier import evaluate


class ClassifierEvaluationTest(unittest.TestCase):
    def test_duplicate_pdfs_do_not_inflate_the_score(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            text = (
                "ДОГОВОР оказания услуг. Исполнитель обязуется выполнить "
                "работу, а Заказчик оплатить услуги. Цена договора, "
                "обязанности сторон и порядок расчетов согласованы."
            )
            with pymupdf.open() as document:
                page = document.new_page()
                font = next((path for path in (
                    Path("C:/Windows/Fonts/arial.ttf"),
                    Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                ) if path.is_file()), None)
                if font is None:
                    raise unittest.SkipTest("A Cyrillic font is required")
                page.insert_font(fontname="cyr", fontfile=str(font))
                page.insert_text((40, 60), text, fontname="cyr")
                pdf_bytes = document.tobytes()

            (root / "first.pdf").write_bytes(pdf_bytes)
            (root / "duplicate.pdf").write_bytes(pdf_bytes)

            with pymupdf.open(stream=pdf_bytes, filetype="pdf") as document:
                extracted = "\n\n".join(
                    page.get_text("text").strip() for page in document
                ).strip()
            digest = hashlib.sha256(extracted.encode("utf-8")).hexdigest()
            manifest = root / "manifest.csv"
            with manifest.open("w", encoding="utf-8", newline="") as file:
                writer = csv.DictWriter(file, fieldnames=["text_sha256", "label"])
                writer.writeheader()
                writer.writerow({"text_sha256": digest, "label": "CONTRACT"})

            results = evaluate(
                root,
                manifest,
                Path("models/document_classifier.joblib"),
            )

            self.assertEqual(2, results["matched_files"])
            self.assertEqual(1, results["known_total"])
            self.assertEqual(1, results["known_correct"])


if __name__ == "__main__":
    unittest.main()
