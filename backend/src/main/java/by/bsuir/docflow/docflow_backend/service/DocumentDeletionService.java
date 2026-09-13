package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentLink;
import by.bsuir.docflow.docflow_backend.entity.StoredFile;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AiProcessingResultRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentLinkRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRouteRepository;
import by.bsuir.docflow.docflow_backend.repository.SedDispatchRepository;
import by.bsuir.docflow.docflow_backend.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class DocumentDeletionService {

    private final DocumentRepository documentRepository;
    private final StoredFileRepository storedFileRepository;
    private final AiProcessingResultRepository aiProcessingResultRepository;
    private final DocumentRouteRepository documentRouteRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final SedDispatchRepository sedDispatchRepository;

    private final Path storageRoot;

    public DocumentDeletionService(
            DocumentRepository documentRepository,
            StoredFileRepository storedFileRepository,
            AiProcessingResultRepository aiProcessingResultRepository,
            DocumentRouteRepository documentRouteRepository,
            DocumentLinkRepository documentLinkRepository,
            SedDispatchRepository sedDispatchRepository,
            @Value("${app.storage.path}") String storagePath
    ) {
        this.documentRepository =
                documentRepository;

        this.storedFileRepository =
                storedFileRepository;

        this.aiProcessingResultRepository =
                aiProcessingResultRepository;

        this.documentRouteRepository =
                documentRouteRepository;

        this.documentLinkRepository =
                documentLinkRepository;

        this.sedDispatchRepository =
                sedDispatchRepository;

        this.storageRoot =
                Paths.get(storagePath)
                        .toAbsolutePath()
                        .normalize();
    }

    @Transactional
    public void deleteDocument(
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

        validateDeletion(
                document
        );

        StoredFile storedFile =
                document.getFile();

        String storagePath =
                storedFile == null
                        ? null
                        : storedFile.getStoragePath();

        documentLinkRepository
                .findByChildDocumentId(documentId)
                .ifPresent(
                        documentLinkRepository::delete
                );

        aiProcessingResultRepository.deleteAll(
                aiProcessingResultRepository
                        .findByDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
        );

        documentRouteRepository.deleteAll(
                documentRouteRepository
                        .findByDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
        );

        sedDispatchRepository.deleteAll(
                sedDispatchRepository
                        .findByDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
        );

        documentRepository.delete(
                document
        );

        documentRepository.flush();

        if (storedFile != null) {

            storedFileRepository.delete(
                    storedFile
            );

            storedFileRepository.flush();
        }

        deletePhysicalFile(
                storagePath
        );
    }

    private void validateDeletion(
            Document document
    ) {

        if (
                document.getStatus()
                        == DocumentStatus.SENT_TO_SED
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ уже отправлен в СЭД и не может быть удалён"
            );
        }

        if (
                !sedDispatchRepository
                        .findByDocumentIdOrderByCreatedAtDesc(
                                document.getId()
                        )
                        .isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ имеет историю отправки в СЭД и не может быть удалён"
            );
        }

        List<DocumentLink> linkedDocuments =
                documentLinkRepository
                        .findByContractDocumentIdOrderByCreatedAtDesc(
                                document.getId()
                        );

        if (!linkedDocuments.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ является основным договором для "
                            + linkedDocuments.size()
                            + " связанного(ых) документа(ов) и не может быть удалён"
            );
        }
    }

    private void deletePhysicalFile(
            String storagePath
    ) {

        if (
                storagePath == null
                        || storagePath.isBlank()
        ) {
            return;
        }

        Path filePath =
                Paths.get(storagePath)
                        .toAbsolutePath()
                        .normalize();

        if (!filePath.startsWith(storageRoot)) {

            throw new IllegalStateException(
                    "Недопустимый путь удаляемого файла"
            );
        }

        try {

            Files.deleteIfExists(
                    filePath
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Не удалось удалить физический файл документа",
                    exception
            );
        }
    }
}