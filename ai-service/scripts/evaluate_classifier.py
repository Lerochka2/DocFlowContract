"""Evaluate a saved classifier on independently labelled local PDFs.

The CSV stores hashes and labels, never document text or customer files.
The same extracted text is counted once even if several PDF copies exist.
"""

import argparse
import csv
import hashlib
import json
from pathlib import Path

import joblib
import pymupdf

from app.services.document_classifier import CONFIDENCE_THRESHOLD


SERVICE_ROOT = Path(__file__).resolve().parents[1]
LABELS = {"CONTRACT", "AMENDMENT_CHANGE", "AMENDMENT_PROLONGATION", "APPENDIX", "OTHER"}


def extract_native_text(pdf_path: Path) -> str:
    with pymupdf.open(pdf_path) as document:
        return "\n\n".join(
            text for page in document
            if (text := page.get_text("text").strip())
        ).strip()


def load_manifest(path: Path) -> dict[str, str]:
    labels = {}
    with path.open("r", encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file):
            digest, label = row["text_sha256"], row["label"]
            if label not in LABELS or len(digest) != 64:
                raise ValueError(f"Invalid evaluation entry: {digest[:12]}")
            if digest in labels and labels[digest] != label:
                raise ValueError(f"Conflicting labels for {digest[:12]}")
            labels[digest] = label
    return labels


def evaluate(pdf_root: Path, manifest_path: Path, model_path: Path) -> dict:
    labels = load_manifest(manifest_path)
    model = joblib.load(model_path)
    seen = set()
    summary = {
        "matched_files": 0,
        "known_total": 0,
        "known_correct": 0,
        "known_accepted": 0,
        "wrong_accepted": 0,
        "other_total": 0,
        "other_accepted": 0,
        "errors": [],
    }

    for pdf_path in sorted(pdf_root.glob("*.pdf")):
        text = extract_native_text(pdf_path)
        digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
        expected = labels.get(digest)
        if expected is None:
            continue
        summary["matched_files"] += 1
        if digest in seen:
            continue
        seen.add(digest)

        if text:
            probabilities = model.predict_proba([text])[0]
            winner = int(probabilities.argmax())
            predicted = str(model.classes_[winner])
            confidence = float(probabilities[winner])
            accepted = confidence >= CONFIDENCE_THRESHOLD
        else:
            predicted, confidence, accepted = None, None, False

        if expected == "OTHER":
            summary["other_total"] += 1
            summary["other_accepted"] += int(accepted)
        else:
            summary["known_total"] += 1
            summary["known_correct"] += int(predicted == expected)
            summary["known_accepted"] += int(accepted)
            summary["wrong_accepted"] += int(accepted and predicted != expected)

        if predicted != expected and expected != "OTHER":
            summary["errors"].append({
                "hash": digest[:12],
                "expected": expected,
                "predicted": predicted,
                "confidence": None if confidence is None else round(confidence, 3),
            })

    if not seen:
        raise ValueError("No labelled PDFs from the manifest were found")
    return summary


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pdf-root", type=Path, default=SERVICE_ROOT.parent / "storage")
    parser.add_argument("--manifest", type=Path,
                        default=SERVICE_ROOT / "data" / "classifier_eval_manifest.csv")
    parser.add_argument("--model", type=Path,
                        default=SERVICE_ROOT / "models" / "document_classifier.joblib")
    args = parser.parse_args()
    print(json.dumps(evaluate(args.pdf_root, args.manifest, args.model),
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
