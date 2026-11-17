package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentLinkRequest;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.SedDispatch;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.repository.DocumentLinkRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.SedDispatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowRulesTest {
    @Mock DocumentRepository documents;
    @Mock DocumentLinkRepository links;
    @Mock SedDispatchRepository dispatches;
    @Mock DocumentAccessService access;
    @Mock DocumentAuditService audit;
    @Mock DocumentRoutingService routing;

    private Document document(long id, DocumentStatus status, DocumentType type) {
        Document value = new Document();
        ReflectionTestUtils.setField(value, "id", id);
        value.setStatus(status);
        value.setDocumentType(type);
        return value;
    }

    @Test void completedDocumentCanBeArchivedAndRestoredWithoutLosingItsIdentity() {
        Document value = document(71L, DocumentStatus.SENT_TO_SED, DocumentType.CONTRACT);
        when(documents.findById(71L)).thenReturn(Optional.of(value));
        when(documents.save(value)).thenReturn(value);
        DocumentArchiveService service = new DocumentArchiveService(documents, access, audit);

        assertEquals("ARCHIVED", service.archive(71L).status());
        assertEquals("SENT_TO_SED", service.restore(71L).status());
        assertEquals(71L, value.getId());
        verify(audit).recordUserEvent(eq(value), eq(DocumentAuditEventType.DOCUMENT_ARCHIVED), anyString(), anyString());
        verify(audit).recordUserEvent(eq(value), eq(DocumentAuditEventType.DOCUMENT_RESTORED), anyString(), anyString());
    }

    @Test void unfinishedDocumentCannotBeArchived() {
        Document value = document(72L, DocumentStatus.UPLOADED, DocumentType.CONTRACT);
        when(documents.findById(72L)).thenReturn(Optional.of(value));
        DocumentArchiveService service = new DocumentArchiveService(documents, access, audit);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.archive(72L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(documents, never()).save(any());
    }

    @Test void documentOutsideArchiveCannotBeRestored() {
        Document value = document(73L, DocumentStatus.SENT_TO_SED, DocumentType.CONTRACT);
        when(documents.findById(73L)).thenReturn(Optional.of(value));
        DocumentArchiveService service = new DocumentArchiveService(documents, access, audit);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.restore(73L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(documents, never()).save(any());
    }

    @Test void sedDispatchRejectsDocumentBeforePreparation() {
        Document value = document(74L, DocumentStatus.APPROVED, DocumentType.CONTRACT);
        when(documents.findById(74L)).thenReturn(Optional.of(value));
        SedDispatchService service = new SedDispatchService(documents, dispatches, audit);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.send(74L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(dispatches, never()).save(any());
    }

    @Test void preparedDocumentChangesToSentAfterDispatch() {
        Document value = document(75L, DocumentStatus.READY_FOR_SED, DocumentType.CONTRACT);
        when(documents.findById(75L)).thenReturn(Optional.of(value));
        when(dispatches.save(any(SedDispatch.class))).thenAnswer(call -> call.getArgument(0));
        SedDispatchService service = new SedDispatchService(documents, dispatches, audit);

        var response = service.send(75L);
        assertEquals("SENT_TO_SED", response.documentStatus());
        assertEquals("SUCCESS", response.dispatchStatus());
        assertTrue(response.externalDocumentId().startsWith("SED-"));
        verify(documents).save(value);
    }

    @Test void additionalDocumentCannotBeLinkedToItself() {
        Document value = document(76L, DocumentStatus.LINK_REQUIRED, DocumentType.APPENDIX);
        when(documents.findById(76L)).thenReturn(Optional.of(value));
        DocumentLinkService service = new DocumentLinkService(documents, links, routing, audit);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.link(76L, new DocumentLinkRequest(76L)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(links, never()).save(any());
    }
}
