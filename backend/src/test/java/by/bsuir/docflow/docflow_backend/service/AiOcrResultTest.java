package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.client.AiClient;
import by.bsuir.docflow.docflow_backend.dto.ai.AiAnalyzeResponse;
import by.bsuir.docflow.docflow_backend.entity.AiProcessingResult;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.StoredFile;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.repository.AiProcessingResultRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiOcrResultTest {
    @ParameterizedTest
    @CsvSource({"true,false,SUCCESS", "true,true,OCR_REQUIRED", "false,false,SUCCESS", "null,false,SUCCESS"})
    void savesOcrMetadataAndKeepsManualReview(String used, boolean requires, String status) throws Exception {
        var documents = mock(DocumentRepository.class);
        var results = mock(AiProcessingResultRepository.class);
        var client = mock(AiClient.class);
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var service = new AiProcessingService(documents, results, client,
                mock(DocumentRoutingService.class), mock(DocumentMetadataExtractionService.class),
                mock(DocumentAuditService.class), transactions);
        var document = new Document();
        ReflectionTestUtils.setField(document, "id", 10L);
        document.setStatus(DocumentStatus.UPLOADED);
        var file = new StoredFile();
        file.setMimeType("application/pdf");
        file.setStoragePath("synthetic-scan.pdf");
        document.setFile(file);
        when(documents.findById(10L)).thenReturn(Optional.of(document));
        // Deserialize the Python API contract, including compatibility with old responses.
        var response = JsonMapper.builder().build().readValue(
                "{\"ocrUsed\":" + used + ",\"requiresOcr\":" + requires
                + ",\"text\":\"ДОГОВОР ПОСТАВКИ\",\"reviewRequired\":true,"
                + "\"classificationAccepted\":false,\"predictedType\":\"CONTRACT\"}",
                AiAnalyzeResponse.class);
        when(client.analyze(any(Path.class))).thenReturn(response);
        when(results.save(any(AiProcessingResult.class))).thenAnswer(invocation -> {
            AiProcessingResult saved = invocation.getArgument(0);
            assertEquals(Boolean.valueOf(used), saved.getOcrUsed());
            assertEquals("ДОГОВОР ПОСТАВКИ", saved.getRecognizedText());
            assertEquals(status, saved.getProcessingStatus().name());
            return saved;
        });
        service.analyze(10L);
        assertEquals(DocumentStatus.REVIEW_REQUIRED, document.getStatus());
        verify(results).save(any(AiProcessingResult.class));
    }
}
