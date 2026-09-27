package by.bsuir.docflow.docflow_backend.dto;

import java.util.List;

public record WorkspaceActivityResponse(

        int days,

        long today,

        long last7Days,

        long last30Days,

        double averagePerDay30,

        long maxDailyDocuments,

        List<WorkspaceActivityDayResponse> activity,

        List<WorkspaceActivityTimeSlotResponse> timeActivity

) {
}