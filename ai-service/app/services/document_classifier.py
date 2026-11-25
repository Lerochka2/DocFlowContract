from pathlib import Path

import joblib


MODEL_PATH = (
    Path(__file__)
    .resolve()
    .parents[2]
    / "models"
    / "document_classifier.joblib"
)

MODEL_VERSION = "tfidf-logreg-v2"
CONFIDENCE_THRESHOLD = 0.80


class DocumentClassifier:

    def __init__(self) -> None:
        if not MODEL_PATH.exists():
            raise RuntimeError(
                f"Модель не найдена: {MODEL_PATH}"
            )

        self.model = joblib.load(MODEL_PATH)

    def classify(self, text: str) -> dict:
        cleaned_text = text.strip()

        if not cleaned_text:
            raise ValueError(
                "Текст документа отсутствует"
            )

        predicted_type = self.model.predict(
            [cleaned_text]
        )[0]

        probabilities = self.model.predict_proba(
            [cleaned_text]
        )[0]

        classes = self.model.classes_

        class_probabilities = {
            document_class: float(probability)
            for document_class, probability
            in zip(classes, probabilities)
        }

        confidence = float(
            class_probabilities[predicted_type]
        )

        return {
            "document_type": predicted_type,
            "confidence": confidence,
            "review_required":
                confidence < CONFIDENCE_THRESHOLD,
            "model_version": MODEL_VERSION,
            "probabilities": class_probabilities,
        }
