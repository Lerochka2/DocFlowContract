package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.SedDispatchResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.SedDispatch;
import by.bsuir.docflow.docflow_backend.repository.SedDispatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SedDispatchQueryService {

    private final SedDispatchRepository sedDispatchRepository;
    private final DocumentAccessService documentAccessService;

    public SedDispatchQueryService(
            SedDispatchRepository sedDispatchRepository,
            DocumentAccessService documentAccessService
    ) {
        this.sedDispatchRepository =
                sedDispatchRepository;

        this.documentAccessService =
                documentAccessService;
    }

    @Transactional(readOnly = true)
    public List<SedDispatchResponse> getHistory(
            Long documentId
    ) {

        documentAccessService
                .requireAccess(
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

        documentAccessService
                .requireAccess(
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