package by.bsuir.docflow.docflow_backend.dto;

import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;

public record ManualReviewRequest(
        DocumentType documentType
) {
}