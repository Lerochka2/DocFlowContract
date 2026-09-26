package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.version.DocumentVersionResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentVersionService;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(
        "/api/documents/{documentId}/versions"
)
public class DocumentVersionController {

    private final DocumentVersionService documentVersionService;


    public DocumentVersionController(
            DocumentVersionService documentVersionService
    ) {
        this.documentVersionService =
                documentVersionService;
    }


    @GetMapping
    public List<DocumentVersionResponse> getVersions(
            @PathVariable Long documentId
    ) {

        return documentVersionService
                .getVersions(
                        documentId
                );
    }


    @PostMapping(
            consumes =
                    MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentVersionResponse uploadNewVersion(
            @PathVariable Long documentId,

            @RequestPart("file")
            MultipartFile file,

            @RequestParam(
                    value = "changeNote",
                    required = false
            )
            String changeNote
    ) {

        return documentVersionService
                .uploadNewVersion(
                        documentId,
                        file,
                        changeNote
                );
    }


    @GetMapping("/{versionId}/file")
    public ResponseEntity<Resource> getVersionFile(
            @PathVariable Long documentId,
            @PathVariable Long versionId
    ) {

        DocumentVersionService.VersionFile file =
                documentVersionService
                        .getVersionFile(
                                documentId,
                                versionId
                        );

        MediaType mediaType =
                MediaType.APPLICATION_PDF;

        if (
                file.mimeType() != null
                        &&
                !file.mimeType().isBlank()
        ) {

            try {

                mediaType =
                        MediaType.parseMediaType(
                                file.mimeType()
                        );

            } catch (
                    IllegalArgumentException ignored
            ) {
            }
        }

        ContentDisposition disposition =
                ContentDisposition
                        .inline()
                        .filename(
                                file.originalName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity
                .ok()
                .contentType(
                        mediaType
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(
                        file.resource()
                );
    }
}