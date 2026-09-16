package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.client.AiClient;
import by.bsuir.docflow.docflow_backend.dto.ai.AiAnalyzeResponse;
import by.bsuir.docflow.docflow_backend.dto.ai.AiProcessingResponse;
import by.bsuir.docflow.docflow_backend.entity.AiProcessingResult;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.AiProcessingStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AiProcessingResultRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiProcessingService {

    private final DocumentRepository documentRepository;
    private final AiProcessingResultRepository aiProcessingResultRepository;
    private final AiClient aiClient;
    private final DocumentRoutingService documentRoutingService;
    private final DocumentMetadataExtractionService
            documentMetadataExtractionService;
    private final DocumentAuditService documentAuditService;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public AiProcessingService(
            DocumentRepository documentRepository,
            AiProcessingResultRepository aiProcessingResultRepository,
            AiClient aiClient,
            DocumentRoutingService documentRoutingService,
            DocumentMetadataExtractionService
                    documentMetadataExtractionService,
            DocumentAuditService documentAuditService,
            PlatformTransactionManager transactionManager
    ) {
        this.documentRepository =
                documentRepository;

        this.aiProcessingResultRepository =
                aiProcessingResultRepository;

        this.aiClient =
                aiClient;

        this.documentRoutingService =
                documentRoutingService;

        this.documentMetadataExtractionService =
                documentMetadataExtractionService;

        this.documentAuditService =
                documentAuditService;

        this.requiresNewTransactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );

        this.requiresNewTransactionTemplate
                .setPropagationBehavior(
                        TransactionDefinition
                                .PROPAGATION_REQUIRES_NEW
                );
    }

    @Transactional
    public AiProcessingResponse analyze(
            Long documentId
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

        validateAnalysisAllowed(
                document
        );

        if (document.getFile() == null) {

            throw new IllegalStateException(
                    "У документа отсутствует связанный файл"
            );
        }

        if (
                !"application/pdf".equalsIgnoreCase(
                        document
                                .getFile()
                                .getMimeType()
                )
        ) {

            throw new IllegalStateException(
                    "AI-анализ пока поддерживает только PDF-файлы"
            );
        }

        Path filePath =
                Path.of(
                        document
                                .getFile()
                                .getStoragePath()
                );

        recordAnalysisStarted(
                documentId
        );

        AiAnalyzeResponse aiResponse;

        try {

            aiResponse =
                    aiClient.analyze(
                            filePath
                    );

        } catch (RuntimeException exception) {

            markProcessingFailed(
                    documentId
            );

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI-сервис временно недоступен"
            );
        }

        boolean classificationAccepted =
                Boolean.TRUE.equals(
                        aiResponse
                                .getClassificationAccepted()
                );

        boolean reviewRequired =
                Boolean.TRUE.equals(
                        aiResponse
                                .getReviewRequired()
                );

        boolean requiresOcr =
                Boolean.TRUE.equals(
                        aiResponse
                                .getRequiresOcr()
                );

        AiProcessingResult result =
                new AiProcessingResult();

        result.setDocument(
                document
        );

        DocumentType predictedType =
                parseDocumentType(
                        aiResponse
                                .getPredictedType()
                );

        result.setPredictedType(
                predictedType
        );

        if (
                aiResponse.getConfidence()
                        != null
        ) {

            result.setConfidence(
                    BigDecimal
                            .valueOf(
                                    aiResponse
                                            .getConfidence()
                            )
                            .setScale(
                                    5,
                                    RoundingMode.HALF_UP
                            )
            );
        }

        result.setClassificationAccepted(
                classificationAccepted
        );

        result.setReviewRequired(
                reviewRequired
        );

        result.setRecognizedText(
                aiResponse.getText()
        );

        result.setOcrUsed(
                false
        );

        result.setModelVersion(
                aiResponse.getModelVersion()
        );

        result.setProbabilities(
                probabilitiesToJson(
                        aiResponse.getProbabilities()
                )
        );

        AiProcessingStatus processingStatus;

        if (requiresOcr) {

            processingStatus =
                    AiProcessingStatus.OCR_REQUIRED;

        } else {

            processingStatus =
                    AiProcessingStatus.SUCCESS;
        }

        result.setProcessingStatus(
                processingStatus
        );

        AiProcessingResult savedResult =
                aiProcessingResultRepository
                        .save(result);

        if (requiresOcr) {

            document.setStatus(
                    DocumentStatus.REVIEW_REQUIRED
            );

        } else if (
                reviewRequired
                        || !classificationAccepted
        ) {

            document.setStatus(
                    DocumentStatus.REVIEW_REQUIRED
            );

        } else if (
                classificationAccepted
                        && aiResponse
                                .getDocumentType()
                        != null
        ) {

            DocumentType acceptedType =
                    parseDocumentType(
                            aiResponse
                                    .getDocumentType()
                    );

            document.setDocumentType(
                    acceptedType
            );

            documentMetadataExtractionService
                    .enrichFromRecognizedText(
                            document,
                            aiResponse.getText()
                    );

            if (
                    acceptedType
                            == DocumentType.CONTRACT
            ) {

                document.setStatus(
                        DocumentStatus.VERIFIED
                );

                documentRepository.save(
                        document
                );

                documentRoutingService.route(
                        document
                );

            } else {

                document.setStatus(
                        DocumentStatus.LINK_REQUIRED
                );
            }
        }

        documentRepository.save(
                document
        );

        documentAuditService.recordSystemEvent(
                document,
                DocumentAuditEventType.AI_ANALYSIS_COMPLETED,
                "AI-анализ документа завершён",
                buildCompletionDetails(
                        aiResponse,
                        processingStatus,
                        document
                )
        );

        return new AiProcessingResponse(
                savedResult.getId(),
                document.getId(),
                aiResponse.getPredictedType(),
                aiResponse.getDocumentType(),
                aiResponse.getConfidence(),
                classificationAccepted,
                reviewRequired,
                requiresOcr,
                aiResponse.getModelVersion(),
                processingStatus.name(),
                aiResponse.getProbabilities(),
                aiResponse.getText()
        );
    }

    private void recordAnalysisStarted(
            Long documentId
    ) {

        requiresNewTransactionTemplate
                .executeWithoutResult(
                        transactionStatus -> {

                            Document auditDocument =
                                    documentRepository
                                            .findById(documentId)
                                            .orElseThrow(
                                                    () ->
                                                            new DocumentNotFoundException(
                                                                    "Документ с ID "
                                                                            + documentId
                                                                            + " не найден"
                                                            )
                                            );

                            String fileName =
                                    auditDocument.getFile() == null
                                            ? "не указан"
                                            : auditDocument
                                                    .getFile()
                                                    .getOriginalName();

                            documentAuditService
                                    .recordSystemEvent(
                                            auditDocument,
                                            DocumentAuditEventType
                                                    .AI_ANALYSIS_STARTED,
                                            "Запущен AI-анализ документа",
                                            "Файл: "
                                                    + fileName
                                                    + "\nСтатус перед анализом: "
                                                    + auditDocument
                                                            .getStatus()
                                                            .name()
                                    );
                        }
                );
    }

    private void markProcessingFailed(
            Long documentId
    ) {

        requiresNewTransactionTemplate
                .executeWithoutResult(
                        transactionStatus -> {

                            Document failedDocument =
                                    documentRepository
                                            .findById(documentId)
                                            .orElseThrow(
                                                    () ->
                                                            new DocumentNotFoundException(
                                                                    "Документ с ID "
                                                                            + documentId
                                                                            + " не найден"
                                                            )
                                            );

                            failedDocument.setStatus(
                                    DocumentStatus.PROCESSING_FAILED
                            );

                            documentRepository.save(
                                    failedDocument
                            );

                            documentAuditService
                                    .recordSystemEvent(
                                            failedDocument,
                                            DocumentAuditEventType
                                                    .AI_ANALYSIS_FAILED,
                                            "AI-анализ документа завершился ошибкой",
                                            "Статус: "
                                                    + DocumentStatus
                                                            .PROCESSING_FAILED
                                                            .name()
                                                    + "\nПричина: AI-сервис недоступен или вернул ошибку"
                                    );
                        }
                );
    }

    private String buildCompletionDetails(
            AiAnalyzeResponse aiResponse,
            AiProcessingStatus processingStatus,
            Document document
    ) {

        return "Предсказанный тип: "
                + safeValue(
                        aiResponse.getPredictedType()
                )
                + "\nИтоговый тип AI: "
                + safeValue(
                        aiResponse.getDocumentType()
                )
                + "\nУверенность: "
                + safeValue(
                        aiResponse.getConfidence()
                )
                + "\nКлассификация принята автоматически: "
                + Boolean.TRUE.equals(
                        aiResponse.getClassificationAccepted()
                )
                + "\nТребуется ручная проверка: "
                + Boolean.TRUE.equals(
                        aiResponse.getReviewRequired()
                )
                + "\nТребуется OCR: "
                + Boolean.TRUE.equals(
                        aiResponse.getRequiresOcr()
                )
                + "\nСтатус обработки: "
                + processingStatus.name()
                + "\nВерсия модели: "
                + safeValue(
                        aiResponse.getModelVersion()
                )
                + "\nСтатус документа после анализа: "
                + (
                        document.getStatus() == null
                                ? "не указан"
                                : document.getStatus().name()
                );
    }

    private String safeValue(
            Object value
    ) {

        if (value == null) {
            return "не указано";
        }

        String text =
                value.toString();

        if (text.isBlank()) {
            return "не указано";
        }

        return text;
    }

    private void validateAnalysisAllowed(
            Document document
    ) {

        DocumentStatus status =
                document.getStatus();

        if (
                status == DocumentStatus.UPLOADED
                        || status
                        == DocumentStatus.PROCESSING_FAILED
        ) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "AI-анализ доступен только для документов со статусом UPLOADED или PROCESSING_FAILED"
        );
    }

    private DocumentType parseDocumentType(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        try {

            return DocumentType.valueOf(
                    value
                            .trim()
                            .toUpperCase()
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new IllegalStateException(
                    "AI-сервис вернул неизвестный тип документа: "
                            + value,
                    exception
            );
        }
    }

    private String probabilitiesToJson(
            Map<String, Double> probabilities
    ) {

        if (
                probabilities == null
                        || probabilities.isEmpty()
        ) {
            return "{}";
        }

        return probabilities
                .entrySet()
                .stream()
                .map(
                        entry ->
                                "\""
                                        + entry.getKey()
                                        + "\":"
                                        + entry.getValue()
                )
                .collect(
                        Collectors.joining(
                                ",",
                                "{",
                                "}"
                        )
                );
    }
}