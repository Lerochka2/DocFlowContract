package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.client.AiClient;
import by.bsuir.docflow.docflow_backend.dto.ContractRiskAnalysisResponse;
import by.bsuir.docflow.docflow_backend.dto.ai.AiRiskAnalysisResponse;
import by.bsuir.docflow.docflow_backend.dto.ai.AiRiskFindingResponse;
import by.bsuir.docflow.docflow_backend.entity.AiProcessingResult;
import by.bsuir.docflow.docflow_backend.entity.ContractRiskAnalysis;
import by.bsuir.docflow.docflow_backend.entity.ContractRiskFinding;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AiProcessingResultRepository;
import by.bsuir.docflow.docflow_backend.repository.ContractRiskAnalysisRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ContractRiskAnalysisService {

    private final DocumentRepository documentRepository;
    private final AiProcessingResultRepository aiProcessingResultRepository;
    private final ContractRiskAnalysisRepository contractRiskAnalysisRepository;
    private final DocumentAccessService documentAccessService;
    private final DocumentAuditService documentAuditService;
    private final AiClient aiClient;

    public ContractRiskAnalysisService(
            DocumentRepository documentRepository,
            AiProcessingResultRepository aiProcessingResultRepository,
            ContractRiskAnalysisRepository contractRiskAnalysisRepository,
            DocumentAccessService documentAccessService,
            DocumentAuditService documentAuditService,
            AiClient aiClient
    ) {
        this.documentRepository =
                documentRepository;

        this.aiProcessingResultRepository =
                aiProcessingResultRepository;

        this.contractRiskAnalysisRepository =
                contractRiskAnalysisRepository;

        this.documentAccessService =
                documentAccessService;

        this.documentAuditService =
                documentAuditService;

        this.aiClient =
                aiClient;
    }

    @Transactional
    public ContractRiskAnalysisResponse analyze(
            Long documentId
    ) {

        Document document =
                getAccessibleContract(
                        documentId
                );

        AiProcessingResult aiResult =
                aiProcessingResultRepository
                        .findByDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
                        .stream()
                        .filter(result ->
                                result.getRecognizedText() != null
                                        &&
                                        !result.getRecognizedText().isBlank()
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Для договора отсутствует распознанный текст. "
                                                + "Сначала необходимо выполнить AI-анализ документа"
                                )
                        );

        AiRiskAnalysisResponse aiResponse =
                aiClient.analyzeRisks(
                        aiResult.getRecognizedText()
                );

        ContractRiskAnalysis analysis =
                new ContractRiskAnalysis();

        analysis.setDocument(
                document
        );

        analysis.setRiskLevel(
                aiResponse.getRiskLevel()
        );

        analysis.setRiskScore(
                aiResponse.getRiskScore()
        );

        analysis.setSummary(
                aiResponse.getSummary()
        );

        analysis.setAnalyzerVersion(
                aiResponse.getAnalyzerVersion()
        );

        if (aiResponse.getRisks() != null) {

            for (
                    AiRiskFindingResponse aiFinding
                    : aiResponse.getRisks()
            ) {

                ContractRiskFinding finding =
                        new ContractRiskFinding();

                finding.setRiskCode(
                        aiFinding.getCode()
                );

                finding.setTitle(
                        aiFinding.getTitle()
                );

                finding.setSeverity(
                        aiFinding.getSeverity()
                );

                finding.setRiskWeight(
                        aiFinding.getWeight()
                );

                finding.setEvidence(
                        aiFinding.getEvidence()
                );

                finding.setExplanation(
                        aiFinding.getExplanation()
                );

                finding.setRecommendation(
                        aiFinding.getRecommendation()
                );

                analysis.addFinding(
                        finding
                );
            }
        }

        ContractRiskAnalysis savedAnalysis =
                contractRiskAnalysisRepository.save(
                        analysis
                );

        int riskCount =
                savedAnalysis
                        .getFindings()
                        .size();

        String auditDetails =
                "Уровень риска: "
                        + savedAnalysis.getRiskLevel()
                        + "; индекс риска: "
                        + savedAnalysis.getRiskScore()
                        + "/100"
                        + "; найдено условий: "
                        + riskCount
                        + "; анализатор: "
                        + savedAnalysis.getAnalyzerVersion();

        documentAuditService.recordUserEvent(
                document,
                DocumentAuditEventType.RISK_ANALYSIS_COMPLETED,
                "Выполнен анализ рисков договора",
                auditDetails
        );

        return toResponse(
                savedAnalysis
        );
    }

    @Transactional(readOnly = true)
    public Optional<ContractRiskAnalysisResponse> findLatest(
            Long documentId
    ) {

        getAccessibleContract(
                documentId
        );

        return contractRiskAnalysisRepository
                .findFirstByDocumentIdOrderByAnalyzedAtDescIdDesc(
                        documentId
                )
                .map(
                        this::toResponse
                );
    }

    private Document getAccessibleContract(
            Long documentId
    ) {

        Document document =
                documentRepository
                        .findById(
                                documentId
                        )
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Документ с ID "
                                                + documentId
                                                + " не найден"
                                )
                        );

        documentAccessService.requireAccess(
                document
        );

        if (
                document.getDocumentType()
                        != DocumentType.CONTRACT
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Анализ рисков доступен только для договоров"
            );
        }

        return document;
    }

    private ContractRiskAnalysisResponse toResponse(
            ContractRiskAnalysis analysis
    ) {

        ContractRiskAnalysisResponse response =
                new ContractRiskAnalysisResponse();

        response.setAnalysisId(
                analysis.getId()
        );

        response.setRiskLevel(
                analysis.getRiskLevel()
        );

        response.setRiskScore(
                analysis.getRiskScore()
        );

        response.setSummary(
                analysis.getSummary()
        );

        response.setAnalyzerVersion(
                analysis.getAnalyzerVersion()
        );

        response.setAnalyzedAt(
                analysis.getAnalyzedAt()
        );

        List<AiRiskFindingResponse> risks =
                new ArrayList<>();

        for (
                ContractRiskFinding finding
                : analysis.getFindings()
        ) {

            AiRiskFindingResponse risk =
                    new AiRiskFindingResponse();

            risk.setCode(
                    finding.getRiskCode()
            );

            risk.setTitle(
                    finding.getTitle()
            );

            risk.setSeverity(
                    finding.getSeverity()
            );

            risk.setWeight(
                    finding.getRiskWeight()
            );

            risk.setEvidence(
                    finding.getEvidence()
            );

            risk.setExplanation(
                    finding.getExplanation()
            );

            risk.setRecommendation(
                    finding.getRecommendation()
            );

            risks.add(
                    risk
            );
        }

        response.setRisks(
                risks
        );

        response.setRiskCount(
                risks.size()
        );

        return response;
    }
}