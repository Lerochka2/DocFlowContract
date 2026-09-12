package by.bsuir.docflow.docflow_backend.dto.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AiProcessingResultResponse(
        Long resultId,
        Long documentId,
        String predictedType,
        BigDecimal confidence,
        boolean classificationAccepted,
        boolean reviewRequired,
        boolean ocrUsed,
        String modelVersion,
        String processingStatus,
        String probabilities,
        String recognizedText,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
}