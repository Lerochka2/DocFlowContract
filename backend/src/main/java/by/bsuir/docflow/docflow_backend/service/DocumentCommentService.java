package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentCommentRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentCommentResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentComment;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentCommentRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DocumentCommentService {

    private final DocumentRepository documentRepository;
    private final DocumentCommentRepository documentCommentRepository;
    private final AppUserRepository appUserRepository;
    private final DocumentAccessService documentAccessService;

    public DocumentCommentService(
            DocumentRepository documentRepository,
            DocumentCommentRepository documentCommentRepository,
            AppUserRepository appUserRepository,
            DocumentAccessService documentAccessService
    ) {
        this.documentRepository =
                documentRepository;

        this.documentCommentRepository =
                documentCommentRepository;

        this.appUserRepository =
                appUserRepository;

        this.documentAccessService =
                documentAccessService;
    }

    @Transactional(readOnly = true)
    public List<DocumentCommentResponse> getComments(
            Long documentId
    ) {

        Document document =
                getAccessibleDocument(
                        documentId
                );

        return documentCommentRepository
                .findByDocument_IdOrderByCreatedAtAsc(
                        document.getId()
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }

    @Transactional
    public DocumentCommentResponse addComment(
            Long documentId,
            DocumentCommentRequest request
    ) {

        Document document =
                getAccessibleDocument(
                        documentId
                );

        String comment =
                normalizeComment(
                        request
                );

        validateComment(
                comment
        );

        AppUser currentUser =
                resolveCurrentUser();

        DocumentComment entity =
                new DocumentComment();

        entity.setDocument(
                document
        );

        entity.setUser(
                currentUser
        );

        entity.setComment(
                comment
        );

        DocumentComment saved =
                documentCommentRepository.save(
                        entity
                );

        return toResponse(
                saved
        );
    }

    private Document getAccessibleDocument(
            Long documentId
    ) {

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(
                                () ->
                                        new DocumentNotFoundException(
                                                "Документ с ID "
                                                        + documentId
                                                        + " не найден"
                                        )
                        );

        documentAccessService.requireAccess(
                document
        );

        return document;
    }

    private DocumentCommentResponse toResponse(
            DocumentComment comment
    ) {

        return new DocumentCommentResponse(
                comment.getId(),
                comment.getDocument().getId(),
                comment.getUser().getId(),
                comment.getUser().getFullName(),
                comment.getComment(),
                comment.getCreatedAt()
        );
    }

    private String normalizeComment(
            DocumentCommentRequest request
    ) {

        if (
                request == null
                        || request.comment() == null
        ) {
            return null;
        }

        String normalized =
                request.comment()
                        .trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private void validateComment(
            String comment
    ) {

        if (
                comment == null
                        || comment.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Комментарий не может быть пустым"
            );
        }

        if (
                comment.length() > 2000
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Комментарий не должен превышать 2000 символов"
            );
        }
    }

    private AppUser resolveCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Пользователь не авторизован"
            );
        }

        return appUserRepository
                .findByLogin(
                        authentication.getName()
                )
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Текущий пользователь не найден"
                                )
                );
    }
}