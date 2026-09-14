package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.ai.AiProcessingResultResponse;
import by.bsuir.docflow.docflow_backend.entity.AiProcessingResult;
import by.bsuir.docflow.docflow_backend.repository.AiProcessingResultRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AiProcessingQueryService {

    private final AiProcessingResultRepository aiProcessingResultRepository;
    private final DocumentAccessService documentAccessService;

    public AiProcessingQueryService(
            AiProcessingResultRepository aiProcessingResultRepository,
            DocumentAccessService documentAccessService
    ) {
        this.aiProcessingResultRepository =
                aiProcessingResultRepository;

        this.documentAccessService =
                documentAccessService;
    }

    @Transactional(readOnly = true)
    public List<AiProcessingResultResponse> getHistory(
            Long documentId
    ) {

        documentAccessService
                .requireAccess(
                        documentId
                );

        return aiProcessingResultRepository
                .findByDocumentIdOrderByCreatedAtDesc(
                        documentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiProcessingResultResponse getLatest(
            Long documentId
    ) {

        documentAccessService
                .requireAccess(
                        documentId
                );

        AiProcessingResult result =
                aiProcessingResultRepository
                        .findFirstByDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Для документа с ID "
                                                + documentId
                                                + " отсутствуют результаты AI-анализа"
                                )
                        );

        return toResponse(
                result
        );
    }

    private AiProcessingResultResponse toResponse(
            AiProcessingResult result
    ) {

        return new AiProcessingResultResponse(
                result.getId(),
                result.getDocument().getId(),
                result.getPredictedType() == null
                        ? null
                        : result.getPredictedType().name(),
                result.getConfidence(),
                Boolean.TRUE.equals(
                        result.getClassificationAccepted()
                ),
                Boolean.TRUE.equals(
                        result.getReviewRequired()
                ),
                Boolean.TRUE.equals(
                        result.getOcrUsed()
                ),
                result.getModelVersion(),
                result.getProcessingStatus() == null
                        ? null
                        : result.getProcessingStatus().name(),
                result.getProbabilities(),
                result.getRecognizedText(),
                result.getProcessedAt(),
                result.getCreatedAt()
        );
    }
}