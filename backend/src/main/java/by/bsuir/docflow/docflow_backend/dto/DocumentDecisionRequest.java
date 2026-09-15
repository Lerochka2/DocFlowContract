package by.bsuir.docflow.docflow_backend.dto;

import by.bsuir.docflow.docflow_backend.entity.enums.DocumentDecision;

public record DocumentDecisionRequest(
        DocumentDecision decision,
        String comment
) {
}