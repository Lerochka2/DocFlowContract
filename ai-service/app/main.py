from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel, Field

from app.services.document_classifier import DocumentClassifier
from app.services.pdf_text_extractor import PdfTextExtractor


MAX_FILE_SIZE = 20 * 1024 * 1024
CONFIDENCE_THRESHOLD = 0.80

pdf_text_extractor = PdfTextExtractor()
document_classifier = DocumentClassifier()


class ClassificationRequest(BaseModel):
    text: str = Field(
        min_length=1,
        description="Распознанный текст документа",
    )


app = FastAPI(
    title="DocFlow Contract AI Service",
    description=(
        "AI microservice for contract document "
        "processing and classification"
    ),
    version="0.5.0",
)


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "OK",
        "service": "DocFlow Contract AI Service",
    }


@app.post("/extract-text")
async def extract_text(
    file: UploadFile = File(...)
) -> dict:
    file_bytes = await read_and_validate_pdf(file)

    try:
        result = pdf_text_extractor.extract(file_bytes)
    except ValueError as exception:
        raise HTTPException(
            status_code=400,
            detail=str(exception),
        ) from exception

    return {
        "filename": file.filename,
        "pageCount": result["page_count"],
        "characterCount": result["character_count"],
        "hasText": result["has_text"],
        "requiresOcr": not result["has_text"],
        "text": result["text"],
    }


@app.post("/classify-text")
def classify_text(
    request: ClassificationRequest
) -> dict:
    try:
        result = document_classifier.classify(
            request.text
        )
    except ValueError as exception:
        raise HTTPException(
            status_code=400,
            detail=str(exception),
        ) from exception

    return format_classification_result(result)


@app.post("/analyze")
async def analyze_document(
    file: UploadFile = File(...)
) -> dict:
    file_bytes = await read_and_validate_pdf(file)

    try:
        extraction_result = pdf_text_extractor.extract(
            file_bytes
        )
    except ValueError as exception:
        raise HTTPException(
            status_code=400,
            detail=str(exception),
        ) from exception

    if not extraction_result["has_text"]:
        return {
            "filename": file.filename,
            "pageCount": extraction_result["page_count"],
            "characterCount": 0,
            "hasText": False,
            "requiresOcr": True,
            "classificationAvailable": False,
            "predictedType": None,
            "documentType": None,
            "confidence": None,
            "classificationAccepted": False,
            "reviewRequired": True,
            "modelVersion": None,
            "probabilities": None,
            "text": "",
        }

    try:
        classification_result = document_classifier.classify(
            extraction_result["text"]
        )
    except ValueError as exception:
        raise HTTPException(
            status_code=400,
            detail=str(exception),
        ) from exception

    formatted = format_classification_result(
        classification_result
    )

    return {
        "filename": file.filename,
        "pageCount": extraction_result["page_count"],
        "characterCount": extraction_result["character_count"],
        "hasText": True,
        "requiresOcr": False,
        "classificationAvailable": True,
        "predictedType": formatted["predictedType"],
        "documentType": formatted["documentType"],
        "confidence": formatted["confidence"],
        "classificationAccepted":
            formatted["classificationAccepted"],
        "reviewRequired": formatted["reviewRequired"],
        "modelVersion": formatted["modelVersion"],
        "probabilities": formatted["probabilities"],
        "text": extraction_result["text"],
    }


async def read_and_validate_pdf(
    file: UploadFile
) -> bytes:
    if file.filename is None or not file.filename.strip():
        raise HTTPException(
            status_code=400,
            detail="Имя файла отсутствует",
        )

    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=400,
            detail=(
                "На данном этапе поддерживаются "
                "только PDF-файлы"
            ),
        )

    if file.content_type != "application/pdf":
        raise HTTPException(
            status_code=400,
            detail="Недопустимый MIME-тип файла",
        )

    file_bytes = await file.read()

    if not file_bytes:
        raise HTTPException(
            status_code=400,
            detail="Файл пуст",
        )

    if len(file_bytes) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=400,
            detail="Размер файла превышает 20 МБ",
        )

    if not file_bytes.startswith(b"%PDF-"):
        raise HTTPException(
            status_code=400,
            detail="Содержимое файла не является PDF",
        )

    return file_bytes


def format_classification_result(
    result: dict
) -> dict:
    predicted_type = result["document_type"]
    confidence = round(
        result["confidence"],
        4,
    )

    accepted = confidence >= CONFIDENCE_THRESHOLD

    return {
        "predictedType": predicted_type,
        "documentType":
            predicted_type if accepted else None,
        "confidence": confidence,
        "classificationAccepted": accepted,
        "reviewRequired": not accepted,
        "modelVersion": result["model_version"],
        "probabilities": {
            label: round(probability, 4)
            for label, probability
            in result["probabilities"].items()
        },
    }