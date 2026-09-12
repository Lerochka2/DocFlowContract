package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.ai.AiProcessingResultResponse;
import by.bsuir.docflow.docflow_backend.service.AiProcessingQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/ai-results")
public class AiProcessingResultController {

    private final AiProcessingQueryService aiProcessingQueryService;

    public AiProcessingResultController(
            AiProcessingQueryService aiProcessingQueryService
    ) {
        this.aiProcessingQueryService = aiProcessingQueryService;
    }

    @GetMapping
    public List<AiProcessingResultResponse> getHistory(
            @PathVariable Long documentId
    ) {
        return aiProcessingQueryService.getHistory(documentId);
    }

    @GetMapping("/latest")
    public AiProcessingResultResponse getLatest(
            @PathVariable Long documentId
    ) {
        return aiProcessingQueryService.getLatest(documentId);
    }
}