package by.bsuir.docflow.docflow_backend.entity.enums;

public enum NotificationType {

    DOCUMENT_ASSIGNED,
    DOCUMENT_RETURNED,
    DOCUMENT_APPROVED,
    DOCUMENT_REJECTED,

    AI_REVIEW_REQUIRED,
    LINK_REQUIRED,

    CONTRACT_EXPIRING,
    CONTRACT_EXPIRED,

    READY_FOR_SED,
    SED_ERROR,

    PROCESSING_ERROR
}
