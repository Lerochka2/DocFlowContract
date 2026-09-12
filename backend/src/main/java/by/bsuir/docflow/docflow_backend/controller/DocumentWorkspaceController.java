package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentWorkspaceItemResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentWorkspaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspace")
public class DocumentWorkspaceController {

    private final DocumentWorkspaceService documentWorkspaceService;

    public DocumentWorkspaceController(
            DocumentWorkspaceService documentWorkspaceService
    ) {
        this.documentWorkspaceService =
                documentWorkspaceService;
    }

    @GetMapping("/documents")
    public List<DocumentWorkspaceItemResponse> getDocuments(
            @RequestParam(required = false)
            String query,

            @RequestParam(required = false)
            String type,

            @RequestParam(required = false)
            String status
    ) {
        return documentWorkspaceService.search(
                query,
                type,
                status
        );
    }
}