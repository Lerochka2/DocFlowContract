package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentSummaryResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentSummaryController {

    private final DocumentSummaryService documentSummaryService;

    public DocumentSummaryController(
            DocumentSummaryService documentSummaryService
    ) {
        this.documentSummaryService =
                documentSummaryService;
    }

    @GetMapping("/{documentId}/summary")
    public DocumentSummaryResponse getSummary(
            @PathVariable Long documentId
    ) {
        return documentSummaryService
                .getSummary(documentId);
    }
}