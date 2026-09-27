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
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
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
    private final DocumentRoutingService documentRoutingService;
    private final AiClient aiClient;

    public ContractRiskAnalysisService(
            DocumentRepository documentRepository,
            AiProcessingResultRepository aiProcessingResultRepository,
            ContractRiskAnalysisRepository contractRiskAnalysisRepository,
            DocumentAccessService documentAccessService,
            DocumentAuditService documentAuditService,
            DocumentRoutingService documentRoutingService,
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

        this.documentRoutingService =
                documentRoutingService;

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
        if (
                document.getFile() == null
                        ||
                document.getFile().getStoragePath() == null
                        ||
                document.getFile().getStoragePath().isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "У документа отсутствует текущий файл для AI-анализа"
            );
        }


        /*
         * ВАЖНО:
         * анализируем именно текущий файл документа.
         *
         * После загрузки v2 document.file уже указывает
         * на новую редакцию, поэтому старый recognizedText
         * от v1 больше не используется.
         */
        by.bsuir.docflow.docflow_backend.dto.ai.AiAnalyzeResponse
                currentDocumentAnalysis =
                aiClient.analyze(
                        java.nio.file.Path.of(
                                document
                                        .getFile()
                                        .getStoragePath()
                        )
                );


        String currentRecognizedText =
                currentDocumentAnalysis.getText();


        if (
                currentRecognizedText == null
                        ||
                currentRecognizedText.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI не удалось получить текст текущей версии договора"
            );
        }


        AiRiskAnalysisResponse aiResponse =
                aiClient.analyzeRisks(
                        currentRecognizedText
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

        if (
                document.getStatus()
                        == DocumentStatus.VERIFIED
        ) {

            documentRoutingService.route(
                    document
            );
        }

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