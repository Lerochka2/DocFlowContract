package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.document.DocumentDetailsResponse;
import by.bsuir.docflow.docflow_backend.dto.document.DocumentSummaryResponse;
import by.bsuir.docflow.docflow_backend.dto.document.DocumentUploadResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.StoredFile;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE =
            20L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(
                    "pdf",
                    "jpg",
                    "jpeg",
                    "png"
            );

    private final StoredFileRepository storedFileRepository;
    private final DocumentRepository documentRepository;
    private final AppUserRepository userRepository;
    private final DocumentAccessService documentAccessService;
    private final Path storageRoot;

    public DocumentService(
            StoredFileRepository storedFileRepository,
            DocumentRepository documentRepository,
            AppUserRepository userRepository,
            DocumentAccessService documentAccessService,
            @Value("${app.storage.path}") String storagePath
    ) {
        this.storedFileRepository =
                storedFileRepository;

        this.documentRepository =
                documentRepository;

        this.userRepository =
                userRepository;

        this.documentAccessService =
                documentAccessService;

        this.storageRoot =
                Paths.get(storagePath)
                        .toAbsolutePath()
                        .normalize();
    }

    @Transactional
    public DocumentUploadResponse upload(
            MultipartFile file,
            Long userId
    ) {

        validateFile(
                file
        );

        AppUser user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Пользователь не найден"
                                        )
                        );

        String originalName =
                file.getOriginalFilename();

        if (
                originalName == null
                        || originalName.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Не удалось определить имя файла"
            );
        }

        String extension =
                getExtension(
                        originalName
                );

        byte[] bytes;

        try {
            bytes =
                    file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Не удалось прочитать загруженный файл",
                    exception
            );
        }

        validateSignature(
                bytes,
                extension
        );

        String checksum =
                calculateSha256(
                        bytes
                );

        StoredFile existingStoredFile =
                storedFileRepository
                        .findFirstBySha256Checksum(
                                checksum
                        )
                        .orElse(null);

        if (existingStoredFile != null) {

            Document existingDocument =
                    documentRepository
                            .findByFileId(
                                    existingStoredFile.getId()
                            )
                            .orElse(null);

            if (existingDocument != null) {

                return new DocumentUploadResponse(
                        existingDocument.getId(),
                        existingStoredFile.getId(),
                        existingStoredFile.getOriginalName(),
                        existingDocument.getStatus() == null
                                ? null
                                : existingDocument
                                        .getStatus()
                                        .name(),
                        true,
                        checksum
                );
            }
        }

        boolean duplicateDetected =
                existingStoredFile != null;

        String storedName =
                UUID.randomUUID()
                        + "."
                        + extension;

        Path target =
                storageRoot
                        .resolve(
                                storedName
                        )
                        .normalize();

        if (
                !target.startsWith(
                        storageRoot
                )
        ) {
            throw new IllegalArgumentException(
                    "Недопустимый путь хранения файла"
            );
        }

        try {

            Files.createDirectories(
                    storageRoot
            );

            Files.write(
                    target,
                    bytes
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Не удалось сохранить файл",
                    exception
            );
        }

        try {

            StoredFile storedFile =
                    new StoredFile();

            storedFile.setOriginalName(
                    originalName
            );

            storedFile.setStoredName(
                    storedName
            );

            storedFile.setStoragePath(
                    target.toString()
            );

            storedFile.setMimeType(
                    file.getContentType()
            );

            storedFile.setSizeBytes(
                    file.getSize()
            );

            storedFile.setSha256Checksum(
                    checksum
            );

            storedFile.setEncrypted(
                    false
            );

            storedFile =
                    storedFileRepository
                            .save(
                                    storedFile
                            );

            Document document =
                    new Document();

            document.setFile(
                    storedFile
            );

            document.setCreatedBy(
                    user
            );

            document.setStatus(
                    DocumentStatus.UPLOADED
            );

            document =
                    documentRepository
                            .save(
                                    document
                            );

            return new DocumentUploadResponse(
                    document.getId(),
                    storedFile.getId(),
                    storedFile.getOriginalName(),
                    document.getStatus().name(),
                    duplicateDetected,
                    checksum
            );

        } catch (RuntimeException exception) {

            try {
                Files.deleteIfExists(
                        target
                );
            } catch (IOException ignored) {
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> getAllDocuments() {

        List<Document> accessibleDocuments =
                documentAccessService
                        .filterAccessibleDocuments(
                                documentRepository
                                        .findAllByOrderByCreatedAtDesc()
                        );

        return accessibleDocuments
                .stream()
                .map(
                        document ->
                                new DocumentSummaryResponse(
                                        document.getId(),
                                        document
                                                .getFile()
                                                .getOriginalName(),
                                        document.getDocumentType() == null
                                                ? null
                                                : document
                                                        .getDocumentType()
                                                        .name(),
                                        document
                                                .getStatus()
                                                .name(),
                                        document
                                                .getDocumentNumber(),
                                        document
                                                .getCreatedAt(),
                                        document
                                                .getCreatedBy()
                                                .getFullName()
                                )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDetailsResponse getDocument(
            Long id
    ) {

        Document document =
                documentRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new DocumentNotFoundException(
                                                "Документ с id "
                                                        + id
                                                        + " не найден"
                                        )
                        );

        documentAccessService
                .requireAccess(
                        document
                );

        AppUser verifiedBy =
                document.getVerifiedBy();

        return new DocumentDetailsResponse(
                document.getId(),

                document
                        .getFile()
                        .getId(),

                document
                        .getFile()
                        .getOriginalName(),

                document
                        .getFile()
                        .getMimeType(),

                document
                        .getFile()
                        .getSizeBytes(),

                document
                        .getFile()
                        .getSha256Checksum(),

                document.getDocumentType() == null
                        ? null
                        : document
                                .getDocumentType()
                                .name(),

                document
                        .getStatus()
                        .name(),

                document
                        .getDocumentNumber(),

                document
                        .getDocumentDate(),

                document
                        .getAmount(),

                document
                        .getCurrency(),

                document
                        .getSubject(),

                document
                        .getValidFrom(),

                document
                        .getValidTo(),

                document
                        .getReferencedContractNumber(),

                document
                        .getReferencedContractDate(),

                document
                        .getCreatedBy()
                        .getId(),

                document
                        .getCreatedBy()
                        .getFullName(),

                verifiedBy == null
                        ? null
                        : verifiedBy.getId(),

                verifiedBy == null
                        ? null
                        : verifiedBy.getFullName(),

                document
                        .getVerifiedAt(),

                document
                        .getCreatedAt(),

                document
                        .getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public DocumentFile getDocumentFile(
            Long id
    ) {

        Document document =
                documentRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new DocumentNotFoundException(
                                                "Документ с id "
                                                        + id
                                                        + " не найден"
                                        )
                        );

        documentAccessService
                .requireAccess(
                        document
                );

        StoredFile storedFile =
                document.getFile();

        if (
                storedFile == null
                        || storedFile.getStoragePath() == null
                        || storedFile.getStoragePath().isBlank()
        ) {
            throw new IllegalStateException(
                    "У документа отсутствует исходный файл"
            );
        }

        Path filePath =
                Paths.get(
                                storedFile.getStoragePath()
                        )
                        .toAbsolutePath()
                        .normalize();

        if (
                !filePath.startsWith(
                        storageRoot
                )
        ) {
            throw new IllegalStateException(
                    "Недопустимый путь к файлу документа"
            );
        }

        if (
                !Files.exists(filePath)
                        || !Files.isRegularFile(filePath)
        ) {
            throw new IllegalStateException(
                    "Файл документа не найден в хранилище"
            );
        }

        try {

            Resource resource =
                    new UrlResource(
                            filePath.toUri()
                    );

            if (
                    !resource.exists()
                            || !resource.isReadable()
            ) {
                throw new IllegalStateException(
                        "Файл документа недоступен для чтения"
                );
            }

            return new DocumentFile(
                    resource,
                    storedFile.getOriginalName(),
                    storedFile.getMimeType()
            );

        } catch (MalformedURLException exception) {

            throw new IllegalStateException(
                    "Не удалось открыть файл документа",
                    exception
            );
        }
    }

    private void validateFile(
            MultipartFile file
    ) {

        if (
                file == null
                        || file.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "Файл не выбран или пуст"
            );
        }

        if (
                file.getSize()
                        > MAX_FILE_SIZE
        ) {
            throw new IllegalArgumentException(
                    "Размер файла превышает 20 МБ"
            );
        }

        String originalName =
                file.getOriginalFilename();

        if (
                originalName == null
                        || originalName.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Имя файла отсутствует"
            );
        }

        String extension =
                getExtension(
                        originalName
                );

        if (
                !ALLOWED_EXTENSIONS
                        .contains(
                                extension
                        )
        ) {
            throw new IllegalArgumentException(
                    "Разрешены только PDF, JPG, JPEG и PNG"
            );
        }

        String mimeType =
                file.getContentType();

        boolean validMime =
                "application/pdf".equals(
                        mimeType
                )
                        || "image/jpeg".equals(
                                mimeType
                        )
                        || "image/png".equals(
                                mimeType
                        );

        if (!validMime) {
            throw new IllegalArgumentException(
                    "Недопустимый MIME-тип файла"
            );
        }
    }

    private String getExtension(
            String fileName
    ) {

        int dotIndex =
                fileName.lastIndexOf(
                        '.'
                );

        if (
                dotIndex < 0
                        || dotIndex
                        == fileName.length() - 1
        ) {
            throw new IllegalArgumentException(
                    "Файл должен иметь расширение"
            );
        }

        return fileName
                .substring(
                        dotIndex + 1
                )
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private void validateSignature(
            byte[] bytes,
            String extension
    ) {

        boolean valid =
                switch (extension) {

                    case "pdf" ->
                            bytes.length >= 5
                                    && bytes[0] == '%'
                                    && bytes[1] == 'P'
                                    && bytes[2] == 'D'
                                    && bytes[3] == 'F'
                                    && bytes[4] == '-';

                    case "jpg", "jpeg" ->
                            bytes.length >= 3
                                    && (bytes[0] & 0xFF) == 0xFF
                                    && (bytes[1] & 0xFF) == 0xD8
                                    && (bytes[2] & 0xFF) == 0xFF;

                    case "png" ->
                            bytes.length >= 8
                                    && (bytes[0] & 0xFF) == 0x89
                                    && bytes[1] == 0x50
                                    && bytes[2] == 0x4E
                                    && bytes[3] == 0x47
                                    && bytes[4] == 0x0D
                                    && bytes[5] == 0x0A
                                    && bytes[6] == 0x1A
                                    && bytes[7] == 0x0A;

                    default ->
                            false;
                };

        if (!valid) {
            throw new IllegalArgumentException(
                    "Содержимое файла не соответствует его расширению"
            );
        }
    }

    private String calculateSha256(
            byte[] bytes
    ) {

        try {

            MessageDigest digest =
                    MessageDigest
                            .getInstance(
                                    "SHA-256"
                            );

            return HexFormat
                    .of()
                    .formatHex(
                            digest.digest(
                                    bytes
                            )
                    );

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 недоступен",
                    exception
            );
        }
    }

    public record DocumentFile(
            Resource resource,
            String originalName,
            String mimeType
    ) {
    }
}