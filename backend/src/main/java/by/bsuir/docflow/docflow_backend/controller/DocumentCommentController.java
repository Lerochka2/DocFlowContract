package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.DocumentCommentRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentCommentResponse;
import by.bsuir.docflow.docflow_backend.service.DocumentCommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentCommentController {

    private final DocumentCommentService documentCommentService;

    public DocumentCommentController(
            DocumentCommentService documentCommentService
    ) {
        this.documentCommentService =
                documentCommentService;
    }

    @GetMapping("/{documentId}/comments")
    public List<DocumentCommentResponse> getComments(
            @PathVariable Long documentId
    ) {

        return documentCommentService
                .getComments(
                        documentId
                );
    }

    @PostMapping("/{documentId}/comments")
    public DocumentCommentResponse addComment(
            @PathVariable Long documentId,
            @RequestBody DocumentCommentRequest request
    ) {

        return documentCommentService
                .addComment(
                        documentId,
                        request
                );
    }
}