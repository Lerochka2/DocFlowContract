package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.ContractRiskAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRiskAnalysisRepository
        extends JpaRepository<ContractRiskAnalysis, Long> {

    Optional<ContractRiskAnalysis>
    findFirstByDocumentIdOrderByAnalyzedAtDescIdDesc(
            Long documentId
    );
}