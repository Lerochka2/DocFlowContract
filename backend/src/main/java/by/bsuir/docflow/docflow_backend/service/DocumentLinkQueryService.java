package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.ContractCandidateResponse;
import by.bsuir.docflow.docflow_backend.dto.DocumentLinkDetailsResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentLink;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentLinkRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class DocumentLinkQueryService {

    private final DocumentLinkRepository documentLinkRepository;
    private final DocumentRepository documentRepository;

    public DocumentLinkQueryService(
            DocumentLinkRepository documentLinkRepository,
            DocumentRepository documentRepository
    ) {
        this.documentLinkRepository =
                documentLinkRepository;
        this.documentRepository =
                documentRepository;
    }

    @Transactional(readOnly = true)
    public DocumentLinkDetailsResponse getContractLink(
            Long childDocumentId
    ) {

        ensureDocumentExists(
                childDocumentId
        );

        DocumentLink link =
                documentLinkRepository
                        .findByChildDocumentId(
                                childDocumentId
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Для документа с ID "
                                                + childDocumentId
                                                + " связь с основным договором отсутствует"
                                )
                        );

        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<DocumentLinkDetailsResponse> getLinkedDocuments(
            Long contractDocumentId
    ) {

        Document contractDocument =
                documentRepository
                        .findById(
                                contractDocumentId
                        )
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + contractDocumentId
                                                + " не найден"
                                )
                        );

        if (
                contractDocument.getDocumentType()
                        != DocumentType.CONTRACT
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Список связанных документов доступен только для документа типа CONTRACT"
            );
        }

        return documentLinkRepository
                .findByContractDocumentIdOrderByCreatedAtDesc(
                        contractDocumentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContractCandidateResponse> getLinkCandidates(
            Long childDocumentId
    ) {

        Document childDocument =
                documentRepository
                        .findById(
                                childDocumentId
                        )
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + childDocumentId
                                                + " не найден"
                                )
                        );

        if (
                childDocument.getStatus()
                        != DocumentStatus.LINK_REQUIRED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Выбор основного договора доступен только для документов со статусом LINK_REQUIRED"
            );
        }

        if (
                childDocument.getDocumentType()
                        == DocumentType.CONTRACT
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Основной договор не требует привязки к другому договору"
            );
        }

        return documentRepository
                .findAll()
                .stream()
                .filter(
                        document ->
                                document.getDocumentType()
                                        == DocumentType.CONTRACT
                )
                .filter(
                        document ->
                                isAvailableContractStatus(
                                        document.getStatus()
                                )
                )
                .sorted(
                        Comparator
                                .comparing(
                                        Document::getId
                                )
                                .reversed()
                )
                .map(this::toCandidateResponse)
                .toList();
    }

    private boolean isAvailableContractStatus(
            DocumentStatus status
    ) {

        if (status == null) {
            return false;
        }

        return status == DocumentStatus.VERIFIED
                || status == DocumentStatus.READY_FOR_SED
                || status == DocumentStatus.SENT_TO_SED;
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

    private ContractCandidateResponse toCandidateResponse(
            Document document
    ) {

        return new ContractCandidateResponse(
                document.getId(),
                document.getDocumentNumber(),
                document.getDocumentDate() == null
                        ? null
                        : document.getDocumentDate().toString(),
                document.getSubject(),
                document.getStatus() == null
                        ? null
                        : document.getStatus().name()
        );
    }

    private DocumentLinkDetailsResponse toResponse(
            DocumentLink link
    ) {

        Document child =
                link.getChildDocument();

        Document contract =
                link.getContractDocument();

        return new DocumentLinkDetailsResponse(
                link.getId(),
                child.getId(),
                child.getDocumentType() == null
                        ? null
                        : child.getDocumentType().name(),
                child.getStatus() == null
                        ? null
                        : child.getStatus().name(),
                contract.getId(),
                contract.getDocumentType() == null
                        ? null
                        : contract.getDocumentType().name(),
                contract.getStatus() == null
                        ? null
                        : contract.getStatus().name(),
                link.getLinkedAt()
        );
    }
}