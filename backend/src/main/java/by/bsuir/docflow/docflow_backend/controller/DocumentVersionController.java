package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.version.DocumentVersionResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentVersionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentVersionController {

    private final DocumentVersionService documentVersionService;

    public DocumentVersionController(
            DocumentVersionService documentVersionService
    ) {
        this.documentVersionService =
                documentVersionService;
    }

    @GetMapping("/{documentId}/versions")
    public List<DocumentVersionResponse> getVersions(
            @PathVariable Long documentId
    ) {
        return documentVersionService.getVersions(
                documentId
        );
    }

    @PostMapping(
            value = "/{documentId}/versions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public DocumentVersionResponse uploadVersion(
            @PathVariable Long documentId,

            @RequestPart("file")
            MultipartFile file,

            @RequestParam(
                    value = "changeNote",
                    required = false
            )
            String changeNote
    ) {
        return documentVersionService.uploadNewVersion(
                documentId,
                file,
                changeNote
        );
    }
}