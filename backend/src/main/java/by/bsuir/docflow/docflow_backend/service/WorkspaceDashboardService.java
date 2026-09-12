package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.WorkspaceDashboardResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkspaceDashboardService {

    private final DocumentRepository documentRepository;

    public WorkspaceDashboardService(
            DocumentRepository documentRepository
    ) {
        this.documentRepository =
                documentRepository;
    }

    @Transactional(readOnly = true)
    public WorkspaceDashboardResponse getDashboard() {

        List<Document> documents =
                documentRepository.findAll();

        return new WorkspaceDashboardResponse(
                documents.size(),

                countByStatus(
                        documents,
                        DocumentStatus.UPLOADED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.AI_PROCESSING
                ),

                countByStatus(
                        documents,
                        DocumentStatus.REVIEW_REQUIRED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.LINK_REQUIRED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.LINKED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.VERIFIED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.READY_FOR_SED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.SENT_TO_SED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.PROCESSING_FAILED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.SED_ERROR
                )
        );
    }

    private long countByStatus(
            List<Document> documents,
            DocumentStatus status
    ) {

        return documents
                .stream()
                .filter(
                        document ->
                                document.getStatus()
                                        == status
                )
                .count();
    }
}