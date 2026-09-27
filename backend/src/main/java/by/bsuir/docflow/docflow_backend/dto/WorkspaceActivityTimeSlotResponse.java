package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDate;

public record WorkspaceActivityTimeSlotResponse(
        LocalDate date,
        int startHour,
        long documentCount
) {
}