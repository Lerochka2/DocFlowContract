package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentComparisonResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentComparisonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentComparisonController {

    private final DocumentComparisonService documentComparisonService;

    public DocumentComparisonController(
            DocumentComparisonService documentComparisonService
    ) {
        this.documentComparisonService =
                documentComparisonService;
    }

    @GetMapping("/{documentId}/comparison")
    public DocumentComparisonResponse compare(
            @PathVariable Long documentId
    ) {

        return documentComparisonService.compare(
                documentId
        );
    }
}
