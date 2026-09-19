package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.ContractRiskAnalysisResponse;
import by.bsuir.docflow.docflow_backend.service.ContractRiskAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class ContractRiskAnalysisController {

    private final ContractRiskAnalysisService contractRiskAnalysisService;

    public ContractRiskAnalysisController(
            ContractRiskAnalysisService contractRiskAnalysisService
    ) {
        this.contractRiskAnalysisService =
                contractRiskAnalysisService;
    }

    @PostMapping("/{documentId}/risk-analysis")
    public ContractRiskAnalysisResponse analyzeRisks(
            @PathVariable Long documentId
    ) {
        return contractRiskAnalysisService
                .analyze(
                        documentId
                );
    }

    @GetMapping("/{documentId}/risk-analysis")
    public ResponseEntity<ContractRiskAnalysisResponse> getLatestAnalysis(
            @PathVariable Long documentId
    ) {
        return contractRiskAnalysisService
                .findLatest(
                        documentId
                )
                .map(
                        ResponseEntity::ok
                )
                .orElseGet(
                        () -> ResponseEntity
                                .noContent()
                                .build()
                );
    }
}