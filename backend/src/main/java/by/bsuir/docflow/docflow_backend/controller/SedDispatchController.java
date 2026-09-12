package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.SedDispatchResponse;
import by.bsuir.docflow.docflow_backend.service.SedDispatchQueryService;
import by.bsuir.docflow.docflow_backend.service.SedDispatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class SedDispatchController {

    private final SedDispatchService sedDispatchService;
    private final SedDispatchQueryService sedDispatchQueryService;

    public SedDispatchController(
            SedDispatchService sedDispatchService,
            SedDispatchQueryService sedDispatchQueryService
    ) {
        this.sedDispatchService =
                sedDispatchService;
        this.sedDispatchQueryService =
                sedDispatchQueryService;
    }

    @PostMapping("/{documentId}/send-to-sed")
    public SedDispatchResponse sendToSed(
            @PathVariable Long documentId
    ) {
        return sedDispatchService.send(
                documentId
        );
    }

    @GetMapping("/{documentId}/sed-dispatches")
    public List<SedDispatchResponse> getHistory(
            @PathVariable Long documentId
    ) {
        return sedDispatchQueryService
                .getHistory(
                        documentId
                );
    }

    @GetMapping("/{documentId}/sed-dispatches/latest")
    public SedDispatchResponse getLatest(
            @PathVariable Long documentId
    ) {
        return sedDispatchQueryService
                .getLatest(
                        documentId
                );
    }
}