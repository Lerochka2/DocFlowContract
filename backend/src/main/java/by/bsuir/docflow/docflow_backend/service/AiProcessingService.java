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
                                "Документ с id "
                                        + documentId
                                        + " не найден"
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
                    "На данном этапе AI-анализ поддерживает только PDF-файлы"
            );
        }

        Path filePath = Path.of(
                document.getFile().getStoragePath()
        );

        AiAnalyzeResponse aiResponse =
                aiClient.analyze(filePath);

        AiProcessingResult processingResult =
                new AiProcessingResult();

        processingResult.setDocument(document);

        DocumentType predictedType =
                parseDocumentType(
                        aiResponse.getPredictedType()
                );

        processingResult.setPredictedType(
                predictedType
        );

        if (aiResponse.getConfidence() != null) {
            processingResult.setConfidence(
                    BigDecimal.valueOf(
                            aiResponse.getConfidence()
                    )
            );
        }

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

        processingResult.setClassificationAccepted(
                classificationAccepted
        );

        processingResult.setReviewRequired(
                reviewRequired
        );

        processingResult.setRecognizedText(
                aiResponse.getText()
        );

        /*
         * OCR ещё не реализован.
         * Поэтому факт необходимости OCR и факт его использования
         * храним отдельно.
         */
        processingResult.setOcrUsed(false);

        processingResult.setModelVersion(
                aiResponse.getModelVersion()
        );

        processingResult.setProbabilities(
                probabilitiesToJson(
                        aiResponse.getProbabilities()
                )
        );

        if (requiresOcr) {
            processingResult.setProcessingStatus(
                    AiProcessingStatus.OCR_REQUIRED
            );
        } else {
            processingResult.setProcessingStatus(
                    AiProcessingStatus.SUCCESS
            );
        }

        AiProcessingResult savedResult =
                aiProcessingResultRepository.save(
                        processingResult
                );

        /*
         * Тип основного документа сохраняем автоматически
         * только при достаточной уверенности модели.
         */
        if (classificationAccepted
                && predictedType != null) {

            document.setDocumentType(
                    predictedType
            );
        }

        /*
         * Документ с низкой уверенностью модели или документ,
         * которому требуется OCR, должен быть проверен человеком.
         */
        if (reviewRequired || requiresOcr) {
            document.setStatus(
                    DocumentStatus.REVIEW_REQUIRED
            );
        }

        documentRepository.save(document);

        String acceptedDocumentType = null;

        if (classificationAccepted
                && predictedType != null) {

            acceptedDocumentType =
                    predictedType.name();
        }

        return new AiProcessingResponse(
                savedResult.getId(),
                document.getId(),
                predictedType == null
                        ? null
                        : predictedType.name(),
                acceptedDocumentType,
                aiResponse.getConfidence(),
                classificationAccepted,
                reviewRequired,
                requiresOcr,
                aiResponse.getModelVersion(),
                savedResult
                        .getProcessingStatus()
                        .name(),
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
                    value.trim()
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
        if (probabilities == null
                || probabilities.isEmpty()) {

            return null;
        }

        return probabilities
                .entrySet()
                .stream()
                .map(entry ->
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