package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.WorkspaceActivityResponse;
import by.bsuir.docflow.docflow_backend.service.WorkspaceActivityService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceActivityController {

    private final WorkspaceActivityService
            workspaceActivityService;


    public WorkspaceActivityController(
            WorkspaceActivityService workspaceActivityService
    ) {

        this.workspaceActivityService =
                workspaceActivityService;
    }


    @GetMapping("/activity")
    public WorkspaceActivityResponse getActivity(
            @RequestParam(
                    defaultValue = "90"
            )
            int days
    ) {

        return workspaceActivityService
                .getActivity(
                        days
                );
    }
}