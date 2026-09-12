package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentRouteResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentRoutingQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/routes")
public class DocumentRouteController {

    private final DocumentRoutingQueryService documentRoutingQueryService;

    public DocumentRouteController(
            DocumentRoutingQueryService documentRoutingQueryService
    ) {
        this.documentRoutingQueryService =
                documentRoutingQueryService;
    }

    @GetMapping
    public List<DocumentRouteResponse> getHistory(
            @PathVariable Long documentId
    ) {
        return documentRoutingQueryService
                .getHistory(documentId);
    }

    @GetMapping("/latest")
    public DocumentRouteResponse getLatest(
            @PathVariable Long documentId
    ) {
        return documentRoutingQueryService
                .getLatest(documentId);
    }
}