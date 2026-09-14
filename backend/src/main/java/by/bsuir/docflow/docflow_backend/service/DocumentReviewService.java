package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.ManualReviewRequest;
import by.bsuir.docflow.docflow_backend.dto.ManualReviewResponse;
import by.bsuir.docflow.docflow_backend.entity.AiProcessingResult;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AiProcessingResultRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentReviewService {

    private final DocumentRepository documentRepository;
    private final DocumentRoutingService documentRoutingService;
    private final AiProcessingResultRepository aiProcessingResultRepository;
    private final DocumentMetadataExtractionService
            documentMetadataExtractionService;

    public DocumentReviewService(
            DocumentRepository documentRepository,
            DocumentRoutingService documentRoutingService,
            AiProcessingResultRepository aiProcessingResultRepository,
            DocumentMetadataExtractionService
                    documentMetadataExtractionService
    ) {
        this.documentRepository =
                documentRepository;

        this.documentRoutingService =
                documentRoutingService;

        this.aiProcessingResultRepository =
                aiProcessingResultRepository;

        this.documentMetadataExtractionService =
                documentMetadataExtractionService;
    }

    @Transactional
    public ManualReviewResponse review(
            Long documentId,
            ManualReviewRequest request
    ) {

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + documentId
                                                + " не найден"
                                )
                        );

        if (
                request == null
                        || request.documentType() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо указать тип документа"
            );
        }

        if (
                document.getStatus()
                        != DocumentStatus.REVIEW_REQUIRED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ручная проверка доступна только для документов со статусом REVIEW_REQUIRED"
            );
        }

        DocumentType selectedType =
                request.documentType();

        document.setDocumentType(
                selectedType
        );

        enrichMetadataFromLatestAiResult(
                document
        );

        if (
                selectedType
                        == DocumentType.CONTRACT
        ) {

            document.setStatus(
                    DocumentStatus.VERIFIED
            );

            documentRepository.save(
                    document
            );

            DocumentRoute route =
                    documentRoutingService.route(
                            document
                    );

            return new ManualReviewResponse(
                    document.getId(),
                    document.getDocumentType().name(),
                    document.getStatus().name(),
                    route.getRoutingTarget().name(),
                    route.getRoutingStatus().name()
            );
        }

        document.setStatus(
                DocumentStatus.LINK_REQUIRED
        );

        documentRepository.save(
                document
        );

        return new ManualReviewResponse(
                document.getId(),
                document.getDocumentType().name(),
                document.getStatus().name(),
                null,
                null
        );
    }

    private void enrichMetadataFromLatestAiResult(
            Document document
    ) {

        AiProcessingResult latestResult =
                aiProcessingResultRepository
                        .findFirstByDocumentIdOrderByCreatedAtDesc(
                                document.getId()
                        )
                        .orElse(null);

        if (latestResult == null) {
            return;
        }

        String recognizedText =
                latestResult.getRecognizedText();

        if (
                recognizedText == null
                        || recognizedText.isBlank()
        ) {
            return;
        }

        documentMetadataExtractionService
                .enrichFromRecognizedText(
                        document,
                        recognizedText
                );
    }
}