import csv
from pathlib import Path

import joblib

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline


RANDOM_STATE = 42

DATASET_PATH = Path("data") / "documents_dataset.csv"
MODEL_PATH = Path("models") / "document_classifier.joblib"


def load_dataset() -> tuple[list[str], list[str]]:
    texts: list[str] = []
    labels: list[str] = []

    with DATASET_PATH.open(
        "r",
        encoding="utf-8",
        newline="",
    ) as csv_file:
        reader = csv.DictReader(csv_file)

        for row in reader:
            text = row["text"].strip()
            label = row["label"].strip()

            if text and label:
                texts.append(text)
                labels.append(label)

    return texts, labels


def build_pipeline() -> Pipeline:
    return Pipeline(
        [
            (
                "tfidf",
                TfidfVectorizer(
                    lowercase=True,
                    ngram_range=(1, 2),
                    min_df=2,
                    max_features=30000,
                    sublinear_tf=True,
                ),
            ),
            (
                "classifier",
                LogisticRegression(
                    max_iter=2000,
                    class_weight="balanced",
                    random_state=RANDOM_STATE,
                ),
            ),
        ]
    )


def main() -> None:
    texts, labels = load_dataset()

    if len(texts) < 40:
        raise RuntimeError(
            "Недостаточно данных для обучения"
        )

    x_train, x_test, y_train, y_test = train_test_split(
        texts,
        labels,
        test_size=0.20,
        random_state=RANDOM_STATE,
        stratify=labels,
    )

    model = build_pipeline()

    print(
        f"Training samples: {len(x_train)}"
    )
    print(
        f"Test samples: {len(x_test)}"
    )

    model.fit(
        x_train,
        y_train,
    )

    predictions = model.predict(x_test)

    accuracy = accuracy_score(
        y_test,
        predictions,
    )

    print()
    print(
        f"Accuracy: {accuracy:.4f}"
    )

    print()
    print(
        "Classification report:"
    )

    print(
        classification_report(
            y_test,
            predictions,
            digits=4,
        )
    )

    print(
        "Confusion matrix:"
    )

    labels_order = sorted(
        set(labels)
    )

    matrix = confusion_matrix(
        y_test,
        predictions,
        labels=labels_order,
    )

    print(
        "Labels:"
    )

    print(
        labels_order
    )

    print(
        matrix
    )

    MODEL_PATH.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    joblib.dump(
        model,
        MODEL_PATH,
    )

    print()
    print(
        f"Model saved: {MODEL_PATH}"
    )


if __name__ == "__main__":
    main()