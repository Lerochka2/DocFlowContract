package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.version.DocumentVersionResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentVersion;
import by.bsuir.docflow.docflow_backend.entity.StoredFile;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentVersionRepository;
import by.bsuir.docflow.docflow_backend.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
public class DocumentVersionService {

    private static final long MAX_FILE_SIZE =
            20L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(
                    "pdf"
            );

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final StoredFileRepository storedFileRepository;
    private final AppUserRepository appUserRepository;
    private final DocumentAccessService documentAccessService;

    private final Path storageRoot;

    public DocumentVersionService(
            DocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository,
            StoredFileRepository storedFileRepository,
            AppUserRepository appUserRepository,
            DocumentAccessService documentAccessService,
            @Value("${app.storage.path}") String storagePath
    ) {
        this.documentRepository =
                documentRepository;

        this.documentVersionRepository =
                documentVersionRepository;

        this.storedFileRepository =
                storedFileRepository;

        this.appUserRepository =
                appUserRepository;

        this.documentAccessService =
                documentAccessService;

        this.storageRoot =
                Paths.get(storagePath)
                        .toAbsolutePath()
                        .normalize();
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> getVersions(
            Long documentId
    ) {

        Document document =
                getDocument(documentId);

        documentAccessService.requireAccess(
                document
        );

        Long currentFileId =
                document.getFile() == null
                        ? null
                        : document.getFile().getId();

        return documentVersionRepository
                .findByDocumentIdOrderByVersionNumberDesc(
                        documentId
                )
                .stream()
                .map(
                        version ->
                                toResponse(
                                        version,
                                        currentFileId
                                )
                )
                .toList();
    }

    @Transactional
    public DocumentVersionResponse uploadNewVersion(
            Long documentId,
            MultipartFile file,
            String changeNote
    ) {

        requireVersionUploadPermission();

        Document document =
                getDocument(documentId);

        documentAccessService.requireAccess(
                document
        );

        if (
                document.getStatus()
                        != DocumentStatus.RETURNED_FOR_CORRECTION
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Новую редакцию можно загрузить только для документа, возвращённого на доработку"
            );
        }

        validateFile(
                file
        );

        String normalizedChangeNote =
                normalizeChangeNote(
                        changeNote
                );

        AppUser currentUser =
                resolveCurrentUser();

        String originalName =
                resolveOriginalName(
                        file
                );

        String extension =
                resolveExtension(
                        originalName
                );

        byte[] bytes;

        try {
            bytes =
                    file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Не удалось прочитать загружаемый файл",
                    exception
            );
        }

        String checksum =
                calculateChecksum(
                        bytes
                );

        StoredFile currentFile =
                document.getFile();

        if (
                currentFile != null
                        && checksum.equalsIgnoreCase(
                        currentFile.getSha256Checksum()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Загруженный файл совпадает с текущей версией документа"
            );
        }

        DocumentVersion latestVersion =
                documentVersionRepository
                        .findFirstByDocumentIdOrderByVersionNumberDesc(
                                documentId
                        )
                        .orElseGet(
                                () ->
                                        createInitialVersion(
                                                document
                                        )
                        );

        int nextVersionNumber =
                latestVersion.getVersionNumber() + 1;

        String storedName =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        + "."
                        + extension;

        Path target =
                storageRoot
                        .resolve(storedName)
                        .normalize();

        if (!target.startsWith(storageRoot)) {
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
                    "Не удалось сохранить новую версию файла",
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
                    resolveMimeType(
                            file,
                            extension
                    )
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
                    storedFileRepository.save(
                            storedFile
                    );

            DocumentVersion version =
                    new DocumentVersion();

            version.setDocument(
                    document
            );

            version.setFile(
                    storedFile
            );

            version.setVersionNumber(
                    nextVersionNumber
            );

            version.setCreatedBy(
                    currentUser
            );

            version.setChangeNote(
                    normalizedChangeNote
            );

            DocumentVersion savedVersion =
                    documentVersionRepository.save(
                            version
                    );

            document.setFile(
                    storedFile
            );

            documentRepository.save(
                    document
            );

            return toResponse(
                    savedVersion,
                    storedFile.getId()
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

    private DocumentVersion createInitialVersion(
            Document document
    ) {

        if (document.getFile() == null) {
            throw new IllegalStateException(
                    "У документа отсутствует исходный файл"
            );
        }

        DocumentVersion version =
                new DocumentVersion();

        version.setDocument(
                document
        );

        version.setFile(
                document.getFile()
        );

        version.setVersionNumber(
                1
        );

        version.setCreatedBy(
                document.getCreatedBy()
        );

        version.setChangeNote(
                "Исходная версия"
        );

        return documentVersionRepository.save(
                version
        );
    }

    private Document getDocument(
            Long documentId
    ) {

        return documentRepository
                .findById(documentId)
                .orElseThrow(
                        () ->
                                new DocumentNotFoundException(
                                        "Документ с ID "
                                                + documentId
                                                + " не найден"
                                )
                );
    }

    @Transactional(readOnly = true)
    public VersionFile getVersionFile(
            Long documentId,
            Long versionId
    ) {

        Document document =
                documentRepository
                        .findById(
                                documentId
                        )
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Документ не найден"
                                        )
                        );

        documentAccessService
                .requireAccess(
                        document
                );

        DocumentVersion version =
                documentVersionRepository
                        .findById(
                                versionId
                        )
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Версия документа не найдена"
                                        )
                        );

        if (
                version.getDocument() == null
                        ||
                !documentId.equals(
                        version.getDocument()
                                .getId()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Версия не относится к указанному документу"
            );
        }

        StoredFile storedFile =
                version.getFile();

        if (
                storedFile == null
                        ||
                storedFile.getStoragePath() == null
                        ||
                storedFile.getStoragePath()
                        .isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Файл версии отсутствует"
            );
        }

        Path filePath =
                Path.of(
                                storedFile
                                        .getStoragePath()
                        )
                        .toAbsolutePath()
                        .normalize();

        if (
                !filePath.startsWith(
                        storageRoot
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Недопустимый путь к файлу версии"
            );
        }

        if (
                !Files.exists(
                        filePath
                )
                        ||
                !Files.isRegularFile(
                        filePath
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Файл версии не найден в хранилище"
            );
        }

        try {

            org.springframework.core.io.Resource resource =
                    new org.springframework.core.io.UrlResource(
                            filePath.toUri()
                    );

            if (
                    !resource.exists()
                            ||
                    !resource.isReadable()
            ) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Файл версии недоступен для чтения"
                );
            }

            return new VersionFile(
                    resource,
                    storedFile.getOriginalName(),
                    storedFile.getMimeType()
            );

        } catch (
                java.net.MalformedURLException exception
        ) {

            throw new IllegalStateException(
                    "Не удалось открыть файл версии",
                    exception
            );
        }
    }


    public record VersionFile(
            org.springframework.core.io.Resource resource,
            String originalName,
            String mimeType
    ) {
    }

    private DocumentVersionResponse toResponse(
            DocumentVersion version,
            Long currentFileId
    ) {

        AppUser createdBy =
                version.getCreatedBy();

        boolean currentVersion =
                currentFileId != null
                        && version.getFile() != null
                        && currentFileId.equals(
                                version.getFile().getId()
                        );

        return new DocumentVersionResponse(
                version.getId(),
                version.getDocument().getId(),
                version.getFile().getId(),
                version.getVersionNumber(),
                version.getFile().getOriginalName(),
                version.getFile().getMimeType(),
                version.getFile().getSizeBytes(),
                version.getFile().getSha256Checksum(),

                createdBy == null
                        ? null
                        : createdBy.getId(),

                createdBy == null
                        ? null
                        : createdBy.getFullName(),

                version.getChangeNote(),
                version.getCreatedAt(),
                currentVersion
        );
    }

    private void validateFile(
            MultipartFile file
    ) {

        if (
                file == null
                        || file.isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо выбрать файл"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Размер файла не должен превышать 20 МБ"
            );
        }

        String originalName =
                resolveOriginalName(
                        file
                );

        String extension =
                resolveExtension(
                        originalName
                );

        if (
                !ALLOWED_EXTENSIONS.contains(
                        extension
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Поддерживаются только PDF-файлы"
            );
        }
    }

    private String resolveOriginalName(
            MultipartFile file
    ) {

        String originalName =
                file.getOriginalFilename();

        if (
                originalName == null
                        || originalName.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Не удалось определить имя файла"
            );
        }

        String safeName =
                Paths.get(
                                originalName
                        )
                        .getFileName()
                        .toString()
                        .trim();

        if (safeName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Некорректное имя файла"
            );
        }

        return safeName;
    }

    private String resolveExtension(
            String fileName
    ) {

        int dotIndex =
                fileName.lastIndexOf('.');

        if (
                dotIndex < 0
                        || dotIndex
                        == fileName.length() - 1
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
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

    private String resolveMimeType(
            MultipartFile file,
            String extension
    ) {

        String mimeType =
                file.getContentType();

        if (
                mimeType != null
                        && !mimeType.isBlank()
        ) {
            return mimeType;
        }

        return "application/pdf";
    }

    private String calculateChecksum(
            byte[] bytes
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
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

    private String normalizeChangeNote(
            String changeNote
    ) {

        if (
                changeNote == null
                        || changeNote.isBlank()
        ) {
            return "Новая редакция после доработки";
        }

        String normalized =
                changeNote.trim();

        if (normalized.length() > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Описание изменений не должно превышать 1000 символов"
            );
        }

        return normalized;
    }

    private AppUser resolveCurrentUser() {

        Authentication authentication =
                getAuthentication();

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

    private void requireVersionUploadPermission() {

        if (
                hasAuthority(
                        "ROLE_ADMIN"
                )
                        || hasAuthority(
                        "ROLE_DOCUMENT_OPERATOR"
                )
        ) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Загружать новую редакцию документа может только делопроизводитель"
        );
    }

    private boolean hasAuthority(
            String authorityName
    ) {

        return getAuthentication()
                .getAuthorities()
                .stream()
                .map(
                        GrantedAuthority::getAuthority
                )
                .anyMatch(
                        authorityName::equals
                );
    }

    private Authentication getAuthentication() {

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

        return authentication;
    }
}