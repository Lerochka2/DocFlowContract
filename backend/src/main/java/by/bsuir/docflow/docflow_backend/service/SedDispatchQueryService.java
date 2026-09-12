package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.SedDispatchResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.SedDispatch;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.SedDispatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SedDispatchQueryService {

    private final SedDispatchRepository sedDispatchRepository;
    private final DocumentRepository documentRepository;

    public SedDispatchQueryService(
            SedDispatchRepository sedDispatchRepository,
            DocumentRepository documentRepository
    ) {
        this.sedDispatchRepository =
                sedDispatchRepository;
        this.documentRepository =
                documentRepository;
    }

    @Transactional(readOnly = true)
    public List<SedDispatchResponse> getHistory(
            Long documentId
    ) {

        ensureDocumentExists(
                documentId
        );

        return sedDispatchRepository
                .findByDocumentIdOrderByCreatedAtDesc(
                        documentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SedDispatchResponse getLatest(
            Long documentId
    ) {

        ensureDocumentExists(
                documentId
        );

        SedDispatch dispatch =
                sedDispatchRepository
                        .findFirstByDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Для документа с ID "
                                                + documentId
                                                + " история отправок в СЭД отсутствует"
                                )
                        );

        return toResponse(
                dispatch
        );
    }

    private void ensureDocumentExists(
            Long documentId
    ) {

        if (!documentRepository.existsById(
                documentId
        )) {
            throw new DocumentNotFoundException(
                    "Документ с ID "
                            + documentId
                            + " не найден"
            );
        }
    }

    private SedDispatchResponse toResponse(
            SedDispatch dispatch
    ) {

        Document document =
                dispatch.getDocument();

        return new SedDispatchResponse(
                dispatch.getId(),
                document.getId(),
                document.getDocumentType() == null
                        ? null
                        : document.getDocumentType().name(),
                document.getStatus() == null
                        ? null
                        : document.getStatus().name(),
                dispatch.getDispatchStatus() == null
                        ? null
                        : dispatch.getDispatchStatus().name(),
                dispatch.getExternalDocumentId(),
                dispatch.getResponseMessage(),
                dispatch.getSentAt()
        );
    }
}