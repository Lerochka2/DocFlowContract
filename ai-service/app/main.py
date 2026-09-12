from fastapi import FastAPI

app = FastAPI(
    title="DocFlow Contract AI Service",
    description="AI microservice for contract document processing and classification",
    version="0.1.0",
)


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "OK",
        "service": "DocFlow Contract AI Service",
    }