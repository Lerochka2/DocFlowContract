package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentMetadataRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentMetadataResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentMetadataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentMetadataController {

    private final DocumentMetadataService documentMetadataService;

    public DocumentMetadataController(
            DocumentMetadataService documentMetadataService
    ) {
        this.documentMetadataService =
                documentMetadataService;
    }

    @GetMapping("/{documentId}/metadata")
    public DocumentMetadataResponse getMetadata(
            @PathVariable Long documentId
    ) {
        return documentMetadataService
                .getMetadata(documentId);
    }

    @PutMapping("/{documentId}/metadata")
    public DocumentMetadataResponse updateMetadata(
            @PathVariable Long documentId,
            @RequestBody DocumentMetadataRequest request
    ) {
        return documentMetadataService
                .updateMetadata(
                        documentId,
                        request
                );
    }
}