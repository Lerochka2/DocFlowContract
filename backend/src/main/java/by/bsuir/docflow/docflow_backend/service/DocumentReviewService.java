package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.ManualReviewRequest;
import by.bsuir.docflow.docflow_backend.dto.ManualReviewResponse;
import by.bsuir.docflow.docflow_backend.entity.AiProcessingResult;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AiProcessingResultRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentReviewService {

    private final DocumentRepository documentRepository;
    private final DocumentRoutingService documentRoutingService;
    private final AiProcessingResultRepository aiProcessingResultRepository;
    private final DocumentMetadataExtractionService
            documentMetadataExtractionService;
    private final DocumentAuditService documentAuditService;

    public DocumentReviewService(
            DocumentRepository documentRepository,
            DocumentRoutingService documentRoutingService,
            AiProcessingResultRepository aiProcessingResultRepository,
            DocumentMetadataExtractionService
                    documentMetadataExtractionService,
            DocumentAuditService documentAuditService
    ) {
        this.documentRepository =
                documentRepository;

        this.documentRoutingService =
                documentRoutingService;

        this.aiProcessingResultRepository =
                aiProcessingResultRepository;

        this.documentMetadataExtractionService =
                documentMetadataExtractionService;

        this.documentAuditService =
                documentAuditService;
    }

    @Transactional
    public ManualReviewResponse review(
            Long documentId,
            ManualReviewRequest request
    ) {

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + documentId
                                                + " не найден"
                                )
                        );

        if (
                request == null
                        || request.documentType() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо указать тип документа"
            );
        }

        if (
                document.getStatus()
                        != DocumentStatus.REVIEW_REQUIRED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ручная проверка доступна только для документов со статусом REVIEW_REQUIRED"
            );
        }

        AiProcessingResult latestAiResult =
                getLatestAiResult(
                        document.getId()
                );

        DocumentType predictedType =
                latestAiResult == null
                        ? null
                        : latestAiResult.getPredictedType();

        DocumentType selectedType =
                request.documentType();

        boolean classificationConfirmed =
                predictedType != null
                        && predictedType == selectedType;

        document.setDocumentType(
                selectedType
        );

        enrichMetadataFromLatestAiResult(
                document,
                latestAiResult
        );

        if (
                selectedType
                        == DocumentType.CONTRACT
        ) {

            document.setStatus(
                    DocumentStatus.VERIFIED
            );

            documentRepository.save(
                    document
            );

            DocumentRoute route =
                    documentRoutingService.route(
                            document
                    );

            recordClassificationAudit(
                    document,
                    predictedType,
                    selectedType,
                    classificationConfirmed
            );

            return new ManualReviewResponse(
                    document.getId(),
                    document.getDocumentType().name(),
                    document.getStatus().name(),
                    route.getRoutingTarget().name(),
                    route.getRoutingStatus().name()
            );
        }

        document.setStatus(
                DocumentStatus.LINK_REQUIRED
        );

        documentRepository.save(
                document
        );

        recordClassificationAudit(
                document,
                predictedType,
                selectedType,
                classificationConfirmed
        );

        return new ManualReviewResponse(
                document.getId(),
                document.getDocumentType().name(),
                document.getStatus().name(),
                null,
                null
        );
    }

    private AiProcessingResult getLatestAiResult(
            Long documentId
    ) {

        return aiProcessingResultRepository
                .findFirstByDocumentIdOrderByCreatedAtDesc(
                        documentId
                )
                .orElse(null);
    }

    private void enrichMetadataFromLatestAiResult(
            Document document,
            AiProcessingResult latestResult
    ) {

        if (latestResult == null) {
            return;
        }

        String recognizedText =
                latestResult.getRecognizedText();

        if (
                recognizedText == null
                        || recognizedText.isBlank()
        ) {
            return;
        }

        documentMetadataExtractionService
                .enrichFromRecognizedText(
                        document,
                        recognizedText
                );
    }

    private void recordClassificationAudit(
            Document document,
            DocumentType predictedType,
            DocumentType selectedType,
            boolean classificationConfirmed
    ) {

        DocumentAuditEventType eventType =
                classificationConfirmed
                        ? DocumentAuditEventType.CLASSIFICATION_CONFIRMED
                        : DocumentAuditEventType.CLASSIFICATION_CHANGED;

        String description =
                classificationConfirmed
                        ? "AI-классификация подтверждена вручную"
                        : "AI-классификация изменена вручную";

        String details =
                "Тип, предложенный AI: "
                        + formatDocumentType(
                                predictedType
                        )
                        + "\nИтоговый тип: "
                        + formatDocumentType(
                                selectedType
                        )
                        + "\nСтатус документа после проверки: "
                        + (
                                document.getStatus() == null
                                        ? "не указан"
                                        : document.getStatus().name()
                        );

        documentAuditService.recordUserEvent(
                document,
                eventType,
                description,
                details
        );
    }

    private String formatDocumentType(
            DocumentType documentType
    ) {

        if (documentType == null) {
            return "не указан";
        }

        return documentType.name();
    }
}