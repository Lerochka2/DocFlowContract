package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.WorkspaceDashboardResponse;
import by.bsuir.docflow.docflow_backend.service.WorkspaceDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceDashboardController {

    private final WorkspaceDashboardService workspaceDashboardService;

    public WorkspaceDashboardController(
            WorkspaceDashboardService workspaceDashboardService
    ) {
        this.workspaceDashboardService =
                workspaceDashboardService;
    }

    @GetMapping("/dashboard")
    public WorkspaceDashboardResponse getDashboard() {
        return workspaceDashboardService
                .getDashboard();
    }
}