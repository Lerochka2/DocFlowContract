package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.ai.AiProcessingResponse;
import by.bsuir.docflow.docflow_backend.dto.document.DocumentDetailsResponse;
import by.bsuir.docflow.docflow_backend.dto.document.DocumentSummaryResponse;
import by.bsuir.docflow.docflow_backend.dto.document.DocumentUploadResponse;
import by.bsuir.docflow.docflow_backend.service.AiProcessingService;
import by.bsuir.docflow.docflow_backend.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AiProcessingService aiProcessingService;

    public DocumentController(
            DocumentService documentService,
            AiProcessingService aiProcessingService
    ) {
        this.documentService = documentService;
        this.aiProcessingService = aiProcessingService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Number userIdClaim =
                jwt.getClaim("userId");

        if (userIdClaim == null) {
            throw new IllegalStateException(
                    "JWT не содержит идентификатор пользователя"
            );
        }

        Long userId =
                userIdClaim.longValue();

        DocumentUploadResponse response =
                documentService.upload(
                        file,
                        userId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public List<DocumentSummaryResponse> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    @GetMapping("/{id}")
    public DocumentDetailsResponse getDocument(
            @PathVariable Long id
    ) {
        return documentService.getDocument(id);
    }

    @PostMapping("/{id}/analyze")
    public AiProcessingResponse analyzeDocument(
            @PathVariable Long id
    ) {
        return aiProcessingService.analyze(id);
    }
}