package by.bsuir.docflow.docflow_backend.dto.ai;

import java.util.Map;

public class AiAnalyzeResponse {

    private String filename;
    private Integer pageCount;
    private Integer characterCount;

    private Boolean hasText;
    private Boolean requiresOcr;
    private Boolean ocrUsed;
    private Boolean classificationAvailable;

    private String predictedType;
    private String documentType;

    private Double confidence;

    private Boolean classificationAccepted;
    private Boolean reviewRequired;

    private String modelVersion;

    private Map<String, Double> probabilities;

    private String text;

    public AiAnalyzeResponse() {
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(Integer characterCount) {
        this.characterCount = characterCount;
    }

    public Boolean getHasText() {
        return hasText;
    }

    public void setHasText(Boolean hasText) {
        this.hasText = hasText;
    }

    public Boolean getRequiresOcr() {
        return requiresOcr;
    }

    public Boolean getOcrUsed() {
        return ocrUsed;
    }

    public void setOcrUsed(Boolean ocrUsed) {
        this.ocrUsed = ocrUsed;
    }

    public void setRequiresOcr(Boolean requiresOcr) {
        this.requiresOcr = requiresOcr;
    }

    public Boolean getClassificationAvailable() {
        return classificationAvailable;
    }

    public void setClassificationAvailable(Boolean classificationAvailable) {
        this.classificationAvailable = classificationAvailable;
    }

    public String getPredictedType() {
        return predictedType;
    }

    public void setPredictedType(String predictedType) {
        this.predictedType = predictedType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Boolean getClassificationAccepted() {
        return classificationAccepted;
    }

    public void setClassificationAccepted(Boolean classificationAccepted) {
        this.classificationAccepted = classificationAccepted;
    }

    public Boolean getReviewRequired() {
        return reviewRequired;
    }

    public void setReviewRequired(Boolean reviewRequired) {
        this.reviewRequired = reviewRequired;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Map<String, Double> getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(
            Map<String, Double> probabilities
    ) {
        this.probabilities = probabilities;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
