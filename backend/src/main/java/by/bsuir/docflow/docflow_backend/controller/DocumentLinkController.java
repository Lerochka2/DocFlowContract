package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentLinkDetailsResponse;
import by.bsuir.docflow.docflow_backend.dto.DocumentLinkRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentLinkResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentLinkQueryService;
import by.bsuir.docflow.docflow_backend.service.DocumentLinkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentLinkController {

    private final DocumentLinkService documentLinkService;
    private final DocumentLinkQueryService documentLinkQueryService;

    public DocumentLinkController(
            DocumentLinkService documentLinkService,
            DocumentLinkQueryService documentLinkQueryService
    ) {
        this.documentLinkService =
                documentLinkService;
        this.documentLinkQueryService =
                documentLinkQueryService;
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

    @GetMapping("/{documentId}/link")
    public DocumentLinkDetailsResponse getContractLink(
            @PathVariable Long documentId
    ) {
        return documentLinkQueryService
                .getContractLink(
                        documentId
                );
    }

    @GetMapping("/{documentId}/linked-documents")
    public List<DocumentLinkDetailsResponse> getLinkedDocuments(
            @PathVariable Long documentId
    ) {
        return documentLinkQueryService
                .getLinkedDocuments(
                        documentId
                );
    }
}