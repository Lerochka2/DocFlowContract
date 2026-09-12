package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentLinkRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentLinkResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentLinkService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentLinkController {

    private final DocumentLinkService documentLinkService;

    public DocumentLinkController(
            DocumentLinkService documentLinkService
    ) {
        this.documentLinkService =
                documentLinkService;
    }

    @PostMapping("/{documentId}/link")
    public DocumentLinkResponse link(
            @PathVariable Long documentId,
            @RequestBody DocumentLinkRequest request
    ) {
        return documentLinkService.link(
                documentId,
                request
        );
    }
}