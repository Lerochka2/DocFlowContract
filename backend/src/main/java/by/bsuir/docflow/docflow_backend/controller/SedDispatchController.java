package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.SedDispatchResponse;
import by.bsuir.docflow.docflow_backend.service.SedDispatchService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class SedDispatchController {

    private final SedDispatchService sedDispatchService;

    public SedDispatchController(
            SedDispatchService sedDispatchService
    ) {
        this.sedDispatchService =
                sedDispatchService;
    }

    @PostMapping("/{documentId}/send-to-sed")
    public SedDispatchResponse sendToSed(
            @PathVariable Long documentId
    ) {
        return sedDispatchService.send(
                documentId
        );
    }
}