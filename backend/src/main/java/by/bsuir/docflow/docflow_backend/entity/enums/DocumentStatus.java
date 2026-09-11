package by.bsuir.docflow.docflow_backend.entity.enums;

public enum DocumentStatus {
    UPLOADED,
    AI_PROCESSING,
    REVIEW_REQUIRED,
    VERIFIED,
    LINK_REQUIRED,
    LINKED,
    READY_FOR_SED,
    SENT_TO_SED,
    PROCESSING_FAILED,
    SED_ERROR,
    ARCHIVED
}