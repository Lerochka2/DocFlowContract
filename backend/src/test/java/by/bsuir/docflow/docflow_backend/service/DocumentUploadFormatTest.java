package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.StoredFile;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.StoredFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DocumentUploadFormatTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final StoredFileRepository files = mock(StoredFileRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(files, documents, users,
                mock(DocumentAccessService.class), mock(DocumentAuditService.class), "storage");
    }

    @ParameterizedTest
    @CsvSource({"scan.png,image/png", "scan.jpg,image/jpeg", "scan.jpeg,image/jpeg"})
    void rejectsImagesAsDocuments(String name, String mime) {
        when(users.findById(1L)).thenReturn(Optional.of(new AppUser()));
        var file = new MockMultipartFile("file", name, mime, new byte[]{1, 2, 3});
        var error = assertThrows(IllegalArgumentException.class, () -> service.upload(file, 1L));
        assertEquals("Поддерживаются только PDF-файлы", error.getMessage());
        verifyNoInteractions(files, documents);
    }

    @Test
    void rejectsImageMimeEvenWithPdfExtension() {
        var file = new MockMultipartFile("file", "scan.pdf", "image/png", new byte[]{1, 2, 3});
        assertThrows(IllegalArgumentException.class, () -> service.upload(file, 1L));
        verifyNoInteractions(files, documents);
    }

    @Test
    void rejectsImageContentRenamedToPdf() {
        when(users.findById(1L)).thenReturn(Optional.of(new AppUser()));
        var file = new MockMultipartFile("file", "scan.pdf", "application/pdf",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
        assertThrows(IllegalArgumentException.class, () -> service.upload(file, 1L));
        verifyNoInteractions(files, documents);
    }

    @Test
    void acceptsPdfAndPreservesDuplicateHandling() {
        when(users.findById(1L)).thenReturn(Optional.of(new AppUser()));
        StoredFile stored = new StoredFile();
        ReflectionTestUtils.setField(stored, "id", 2L);
        stored.setOriginalName("contract.PDF");
        Document document = new Document();
        ReflectionTestUtils.setField(document, "id", 3L);
        when(files.findFirstBySha256Checksum(anyString())).thenReturn(Optional.of(stored));
        when(documents.findByFileId(2L)).thenReturn(Optional.of(document));
        var file = new MockMultipartFile("file", "contract.PDF", "application/pdf",
                "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII));
        var result = service.upload(file, 1L);
        assertEquals(3L, result.getDocumentId());
        assertTrue(result.isDuplicateDetected());
    }
}
