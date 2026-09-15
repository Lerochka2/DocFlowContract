package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.service.DocumentCorrectionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentCorrectionController {

    private final DocumentCorrectionService documentCorrectionService;

    public DocumentCorrectionController(
            DocumentCorrectionService documentCorrectionService
    ) {
        this.documentCorrectionService =
                documentCorrectionService;
    }

    @PostMapping("/{documentId}/resubmit-for-review")
    public DocumentStatus resubmitForReview(
            @PathVariable Long documentId
    ) {
        return documentCorrectionService
                .resubmitForReview(
                        documentId
                );
    }
}