package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentAuditEventResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentAuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentAuditController {

    private final DocumentAuditService documentAuditService;

    public DocumentAuditController(
            DocumentAuditService documentAuditService
    ) {
        this.documentAuditService =
                documentAuditService;
    }

    @GetMapping("/{documentId}/audit")
    public List<DocumentAuditEventResponse> getAuditHistory(
            @PathVariable Long documentId
    ) {

        return documentAuditService
                .getHistory(
                        documentId
                );
    }
}