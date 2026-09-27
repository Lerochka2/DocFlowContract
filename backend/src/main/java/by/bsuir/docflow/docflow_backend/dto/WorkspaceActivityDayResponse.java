package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDate;

public record WorkspaceActivityDayResponse(
        LocalDate date,
        long documentCount
) {
}