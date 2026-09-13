package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.ai.AiProcessingResponse;
import by.bsuir.docflow.docflow_backend.dto.document.DocumentDetailsResponse;
import by.bsuir.docflow.docflow_backend.dto.document.DocumentSummaryResponse;
import by.bsuir.docflow.docflow_backend.dto.document.DocumentUploadResponse;
import by.bsuir.docflow.docflow_backend.service.AiProcessingService;
import by.bsuir.docflow.docflow_backend.service.DocumentDeletionService;
import by.bsuir.docflow.docflow_backend.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AiProcessingService aiProcessingService;
    private final DocumentDeletionService documentDeletionService;

    public DocumentController(
            DocumentService documentService,
            AiProcessingService aiProcessingService,
            DocumentDeletionService documentDeletionService
    ) {
        this.documentService =
                documentService;

        this.aiProcessingService =
                aiProcessingService;

        this.documentDeletionService =
                documentDeletionService;
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

        return documentService
                .getAllDocuments();
    }

    @GetMapping("/{id}")
    public DocumentDetailsResponse getDocument(
            @PathVariable Long id
    ) {

        return documentService
                .getDocument(id);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getDocumentFile(
            @PathVariable Long id
    ) {

        DocumentService.DocumentFile documentFile =
                documentService
                        .getDocumentFile(id);

        MediaType mediaType =
                resolveMediaType(
                        documentFile.mimeType()
                );

        ContentDisposition contentDisposition =
                ContentDisposition
                        .inline()
                        .filename(
                                documentFile.originalName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity
                .ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(
                        documentFile.resource()
                );
    }

    @PostMapping("/{id}/analyze")
    public AiProcessingResponse analyzeDocument(
            @PathVariable Long id
    ) {

        return aiProcessingService
                .analyze(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id
    ) {

        documentDeletionService
                .deleteDocument(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    private MediaType resolveMediaType(
            String mimeType
    ) {

        if (
                mimeType == null
                        ||
                        mimeType.isBlank()
        ) {

            return MediaType
                    .APPLICATION_OCTET_STREAM;
        }

        try {

            return MediaType
                    .parseMediaType(
                            mimeType
                    );

        } catch (
                IllegalArgumentException exception
        ) {

            return MediaType
                    .APPLICATION_OCTET_STREAM;
        }
    }
}