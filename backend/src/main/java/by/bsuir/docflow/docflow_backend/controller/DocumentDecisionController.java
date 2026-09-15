package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentDecisionRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentDecisionResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentDecisionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentDecisionController {

    private final DocumentDecisionService documentDecisionService;

    public DocumentDecisionController(
            DocumentDecisionService documentDecisionService
    ) {
        this.documentDecisionService =
                documentDecisionService;
    }

    @PostMapping("/{documentId}/decision")
    public DocumentDecisionResponse decide(
            @PathVariable Long documentId,
            @RequestBody DocumentDecisionRequest request
    ) {
        return documentDecisionService
                .decide(
                        documentId,
                        request
                );
    }
}