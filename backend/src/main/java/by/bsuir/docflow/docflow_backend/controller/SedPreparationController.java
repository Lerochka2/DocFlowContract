package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.SedPreparationResponse;
import by.bsuir.docflow.docflow_backend.service.SedPreparationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class SedPreparationController {

    private final SedPreparationService sedPreparationService;

    public SedPreparationController(
            SedPreparationService sedPreparationService
    ) {
        this.sedPreparationService =
                sedPreparationService;
    }

    @PostMapping("/{documentId}/prepare-for-sed")
    public SedPreparationResponse prepareForSed(
            @PathVariable Long documentId
    ) {
        return sedPreparationService
                .prepare(documentId);
    }
}