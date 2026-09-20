package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentArchiveResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentArchiveService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentArchiveController {

    private final DocumentArchiveService documentArchiveService;

    public DocumentArchiveController(
            DocumentArchiveService documentArchiveService
    ) {
        this.documentArchiveService =
                documentArchiveService;
    }

    @PostMapping("/{documentId}/archive")
    public DocumentArchiveResponse archive(
            @PathVariable Long documentId
    ) {

        return documentArchiveService
                .archive(
                        documentId
                );
    }

    @PostMapping("/{documentId}/restore")
    public DocumentArchiveResponse restore(
            @PathVariable Long documentId
    ) {

        return documentArchiveService
                .restore(
                        documentId
                );
    }
}