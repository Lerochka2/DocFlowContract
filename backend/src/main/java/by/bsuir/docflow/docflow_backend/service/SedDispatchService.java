package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.SedDispatchResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.SedDispatch;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.SedDispatchStatus;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.SedDispatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SedDispatchService {

    private final DocumentRepository documentRepository;
    private final SedDispatchRepository sedDispatchRepository;
    private final DocumentAuditService documentAuditService;

    public SedDispatchService(
            DocumentRepository documentRepository,
            SedDispatchRepository sedDispatchRepository,
            DocumentAuditService documentAuditService
    ) {
        this.documentRepository =
                documentRepository;

        this.sedDispatchRepository =
                sedDispatchRepository;

        this.documentAuditService =
                documentAuditService;
    }

    @Transactional
    public SedDispatchResponse send(
            Long documentId
    ) {

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + documentId
                                                + " не найден"
                                )
                        );

        if (
                document.getStatus()
                        != DocumentStatus.READY_FOR_SED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Отправка в СЭД доступна только для документов со статусом READY_FOR_SED"
            );
        }

        LocalDateTime sentAt =
                LocalDateTime.now();

        String externalDocumentId =
                generateExternalDocumentId();

        SedDispatch dispatch =
                new SedDispatch();

        dispatch.setDocument(
                document
        );

        dispatch.setDispatchStatus(
                SedDispatchStatus.SUCCESS
        );

        dispatch.setExternalDocumentId(
                externalDocumentId
        );

        dispatch.setResponseMessage(
                "Документ успешно зарегистрирован во внешней СЭД"
        );

        dispatch.setSentAt(
                sentAt
        );

        SedDispatch savedDispatch =
                sedDispatchRepository.save(
                        dispatch
                );

        document.setStatus(
                DocumentStatus.SENT_TO_SED
        );

        documentRepository.save(
                document
        );

        documentAuditService.recordUserEvent(
                document,
                DocumentAuditEventType.SENT_TO_SED,
                "Документ отправлен в корпоративную СЭД",
                "Внешний ID: "
                        + externalDocumentId
                        + "\nСтатус отправки: "
                        + savedDispatch.getDispatchStatus().name()
                        + "\nОтвет СЭД: "
                        + savedDispatch.getResponseMessage()
        );

        return toResponse(
                savedDispatch
        );
    }

    private String generateExternalDocumentId() {

        String value =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();

        return "SED-" + value;
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
                dispatch.getDispatchStatus().name(),
                dispatch.getExternalDocumentId(),
                dispatch.getResponseMessage(),
                dispatch.getSentAt()
        );
    }
}