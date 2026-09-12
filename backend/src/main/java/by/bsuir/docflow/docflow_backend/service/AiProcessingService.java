package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.client.AiClient;
import by.bsuir.docflow.docflow_backend.dto.ai.AiAnalyzeResponse;
import by.bsuir.docflow.docflow_backend.dto.ai.AiProcessingResponse;
import by.bsuir.docflow.docflow_backend.entity.AiProcessingResult;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.AiProcessingStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AiProcessingResultRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AiProcessingService(
            DocumentRepository documentRepository,
            AiProcessingResultRepository aiProcessingResultRepository,
            AiClient aiClient
    ) {
        this.documentRepository = documentRepository;
        this.aiProcessingResultRepository = aiProcessingResultRepository;
        this.aiClient = aiClient;
    }

    @Transactional
    public AiProcessingResponse analyze(Long documentId) {

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(
                        () -> new DocumentNotFoundException(
                                "Документ с ID " + documentId + " не найден"
                        )
                );

        if (document.getFile() == null) {
            throw new IllegalStateException(
                    "У документа отсутствует связанный файл"
            );
        }

        if (!"application/pdf".equalsIgnoreCase(
                document.getFile().getMimeType()
        )) {
            throw new IllegalStateException(
                    "AI-анализ пока поддерживает только PDF-файлы"
            );
        }

        Path filePath = Path.of(
                document.getFile().getStoragePath()
        );

        AiAnalyzeResponse aiResponse =
                aiClient.analyze(filePath);

        boolean classificationAccepted =
                Boolean.TRUE.equals(
                        aiResponse.getClassificationAccepted()
                );

        boolean reviewRequired =
                Boolean.TRUE.equals(
                        aiResponse.getReviewRequired()
                );

        boolean requiresOcr =
                Boolean.TRUE.equals(
                        aiResponse.getRequiresOcr()
                );

        AiProcessingResult result =
                new AiProcessingResult();

        result.setDocument(document);

        DocumentType predictedType =
                parseDocumentType(
                        aiResponse.getPredictedType()
                );

        result.setPredictedType(
                predictedType
        );

        if (aiResponse.getConfidence() != null) {
            result.setConfidence(
                    BigDecimal
                            .valueOf(
                                    aiResponse.getConfidence()
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

        result.setOcrUsed(false);

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

        /*
         * Бизнес-логика изменения состояния документа
         * после обработки AI-сервисом.
         */
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
                        && aiResponse.getDocumentType() != null
        ) {

            DocumentType acceptedType =
                    parseDocumentType(
                            aiResponse.getDocumentType()
                    );

            document.setDocumentType(
                    acceptedType
            );

            document.setStatus(
                    DocumentStatus.VERIFIED
            );
        }

        documentRepository.save(
                document
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

    private DocumentType parseDocumentType(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return DocumentType.valueOf(
                    value
                            .trim()
                            .toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
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