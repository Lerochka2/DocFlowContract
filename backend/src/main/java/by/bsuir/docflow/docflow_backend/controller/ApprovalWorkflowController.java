package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.approval.ApprovalStepResponse;
import by.bsuir.docflow.docflow_backend.service.ApprovalWorkflowService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approval")
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;


    public ApprovalWorkflowController(
            ApprovalWorkflowService approvalWorkflowService
    ) {
        this.approvalWorkflowService =
                approvalWorkflowService;
    }


    @PostMapping(
            "/documents/{documentId}/initialize"
    )
    public List<ApprovalStepResponse> initialize(
            @PathVariable
            Long documentId
    ) {

        return approvalWorkflowService
                .initializeFromCurrentRoute(
                        documentId
                );
    }


    @GetMapping(
            "/documents/{documentId}/steps"
    )
    public List<ApprovalStepResponse> getSteps(
            @PathVariable
            Long documentId
    ) {

        return approvalWorkflowService
                .getDocumentSteps(
                        documentId
                );
    }


    @GetMapping("/my")
    public List<ApprovalStepResponse> getMyQueue() {

        return approvalWorkflowService
                .getMyQueue();
    }


    @PostMapping(
            "/steps/{stepId}/claim"
    )
    public ApprovalStepResponse claim(
            @PathVariable
            Long stepId
    ) {

        return approvalWorkflowService
                .claim(
                        stepId
                );
    }
}