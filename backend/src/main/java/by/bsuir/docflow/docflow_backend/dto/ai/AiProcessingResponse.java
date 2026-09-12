package by.bsuir.docflow.docflow_backend.dto.ai;

import java.util.Map;

public class AiProcessingResponse {

    private final Long resultId;
    private final Long documentId;
    private final String predictedType;
    private final String documentType;
    private final Double confidence;
    private final Boolean classificationAccepted;
    private final Boolean reviewRequired;
    private final Boolean requiresOcr;
    private final String modelVersion;
    private final String processingStatus;
    private final Map<String, Double> probabilities;
    private final String recognizedText;

    public AiProcessingResponse(
            Long resultId,
            Long documentId,
            String predictedType,
            String documentType,
            Double confidence,
            Boolean classificationAccepted,
            Boolean reviewRequired,
            Boolean requiresOcr,
            String modelVersion,
            String processingStatus,
            Map<String, Double> probabilities,
            String recognizedText
    ) {
        this.resultId = resultId;
        this.documentId = documentId;
        this.predictedType = predictedType;
        this.documentType = documentType;
        this.confidence = confidence;
        this.classificationAccepted = classificationAccepted;
        this.reviewRequired = reviewRequired;
        this.requiresOcr = requiresOcr;
        this.modelVersion = modelVersion;
        this.processingStatus = processingStatus;
        this.probabilities = probabilities;
        this.recognizedText = recognizedText;
    }

    public Long getResultId() {
        return resultId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getPredictedType() {
        return predictedType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public Double getConfidence() {
        return confidence;
    }

    public Boolean getClassificationAccepted() {
        return classificationAccepted;
    }

    public Boolean getReviewRequired() {
        return reviewRequired;
    }

    public Boolean getRequiresOcr() {
        return requiresOcr;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public Map<String, Double> getProbabilities() {
        return probabilities;
    }

    public String getRecognizedText() {
        return recognizedText;
    }
}