package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.ManualReviewRequest;
import by.bsuir.docflow.docflow_backend.dto.ManualReviewResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentReviewService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentReviewController {

    private final DocumentReviewService documentReviewService;

    public DocumentReviewController(
            DocumentReviewService documentReviewService
    ) {
        this.documentReviewService =
                documentReviewService;
    }

    @PostMapping("/{documentId}/review")
    public ManualReviewResponse review(
            @PathVariable Long documentId,
            @RequestBody ManualReviewRequest request
    ) {
        return documentReviewService.review(
                documentId,
                request
        );
    }
}