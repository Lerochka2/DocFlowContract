package by.bsuir.docflow.docflow_backend.entity;

import by.bsuir.docflow.docflow_backend.entity.enums.AiProcessingStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_processing_results")
public class AiProcessingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_type", length = 50)
    private DocumentType predictedType;

    @Column(name = "confidence", precision = 6, scale = 5)
    private BigDecimal confidence;

    @Column(name = "classification_accepted", nullable = false)
    private Boolean classificationAccepted = false;

    @Column(name = "review_required", nullable = false)
    private Boolean reviewRequired = true;

    @Lob
    @Column(name = "recognized_text", columnDefinition = "LONGTEXT")
    private String recognizedText;

    @Column(name = "ocr_used", nullable = false)
    private Boolean ocrUsed = false;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "probabilities", columnDefinition = "JSON")
    private String probabilities;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    private AiProcessingStatus processingStatus;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AiProcessingResult() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (processedAt == null) {
            processedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (classificationAccepted == null) {
            classificationAccepted = false;
        }

        if (reviewRequired == null) {
            reviewRequired = true;
        }

        if (ocrUsed == null) {
            ocrUsed = false;
        }
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public DocumentType getPredictedType() {
        return predictedType;
    }

    public void setPredictedType(DocumentType predictedType) {
        this.predictedType = predictedType;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
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

    public String getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }

    public Boolean getOcrUsed() {
        return ocrUsed;
    }

    public void setOcrUsed(Boolean ocrUsed) {
        this.ocrUsed = ocrUsed;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(String probabilities) {
        this.probabilities = probabilities;
    }

    public AiProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(AiProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}