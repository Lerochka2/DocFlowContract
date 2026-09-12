from fastapi import FastAPI, File, HTTPException, UploadFile

from app.services.pdf_text_extractor import PdfTextExtractor


MAX_FILE_SIZE = 20 * 1024 * 1024

pdf_text_extractor = PdfTextExtractor()


app = FastAPI(
    title="DocFlow Contract AI Service",
    description=(
        "AI microservice for contract document "
        "processing and classification"
    ),
    version="0.2.0",
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
    if file.filename is None or not file.filename.strip():
        raise HTTPException(
            status_code=400,
            detail="Имя файла отсутствует",
        )

    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=400,
            detail="На данном этапе поддерживаются только PDF-файлы",
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