/**
 * Author - Pavan
 */
package com.ideatrack.main.controller;


import com.ideatrack.main.dto.PagedResponse;
import com.ideatrack.main.dto.reviewer.ProgressionDTO;
import com.ideatrack.main.dto.reviewer.ReviewerDashboardDTO;
import com.ideatrack.main.dto.reviewer.ReviewerDecisionRequest;
import com.ideatrack.main.dto.reviewer.ReviewerDiscussionDTO;
import com.ideatrack.main.dto.reviewer.ReviewerDiscussionRequestDTO;
import com.ideatrack.main.service.ProgressionService;
import com.ideatrack.main.service.ReviewerAssignmentService;
import com.ideatrack.main.service.ReviewerDashboardService;
import com.ideatrack.main.service.ReviewerDecisionService;
import com.ideatrack.main.service.ReviewerDiscussionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/reviewer", produces = "application/json")
@RequiredArgsConstructor
@Slf4j
@Validated
@PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN','REVIEWER')")
public class ReviewerController{

    private final ReviewerAssignmentService assignmentService;
    private final ReviewerDashboardService dashboardService;
    private final ReviewerDecisionService decisionService;
    private final ReviewerDiscussionService discussionService;
    private final ProgressionService progressionService;


    // 1) DECISION: Submit decision 

    @PostMapping(value = "/ideas/{ideaId}/decision", consumes = "application/json",produces = "application/json")
    public ResponseEntity<String> submitDecision(
            @PathVariable Integer ideaId,
            @Valid @RequestBody ReviewerDecisionRequest request) {

        decisionService.processDecision(ideaId, request);
        return ResponseEntity.ok("\"Decision processed successfully\""); 
    }
    
	@GetMapping("/idea/{ideaId}/decisions")
	public ResponseEntity<List<ReviewerDecisionRequest>> getDecisions(@PathVariable Integer ideaId) {
    	return ResponseEntity.ok(decisionService.getReviewerDecisions(ideaId));
	}



    // 2) DASHBOARD: Reviewer dashboard (filter = ALL/UNDERREVIEW/ACCEPTED/REJECTED/REFINE/PENDING etc.)
	@PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN','REVIEWER')")
    @GetMapping("/me/dashboard")
    public ResponseEntity<List<ReviewerDashboardDTO>> getDashboard(
            @RequestParam(required = false, defaultValue = "ALL") String filter) {

        return ResponseEntity.ok(dashboardService.getReviewerDashboard(filter));
    }


    // 3) DISCUSSION: Post comment / reply in current stage

    @PostMapping(value = "/ideas/{ideaId}/discussions", consumes = "application/json",produces = "application/json")
    public ResponseEntity<String> postDiscussion(
            @PathVariable Integer ideaId,
            @Valid @RequestBody ReviewerDiscussionRequestDTO request) {

        discussionService.postDiscussion(
                ideaId,
                request.getUserId(),
                request.getStageId(),
                request.getText(),
                request.getReplyParent()
        );

        return ResponseEntity.ok("\"Discussion posted\"");
    }


    // 4) DISCUSSION: Get all discussions for a stage (non-paged)

    @GetMapping("/ideas/{ideaId}/discussions")
    public ResponseEntity<List<ReviewerDiscussionDTO>> getDiscussions(
            @PathVariable Integer ideaId,
            @RequestParam @NotNull @Min(1) Integer stageId) {

        return ResponseEntity.ok(discussionService.getDiscussionsForStage(ideaId, stageId));
    }


    // 5) DISCUSSION: Paged discussions (for UI screens)

    @GetMapping("/ideas/{ideaId}/discussions/page")
    public ResponseEntity<PagedResponse<ReviewerDiscussionDTO>> getDiscussionsPaged(
            @PathVariable Integer ideaId,
            @RequestParam @NotNull @Min(1) Integer stageId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt,asc") String sort) {

        Pageable pageable = toPageable(page, size, sort);
        return ResponseEntity.ok(discussionService.getDiscussionsForStagePaged(ideaId, stageId, pageable));
    }

    // 6) JOB: Manual trigger for EOD auto assignment (optional utility endpoint)
    @PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN')")
    @PostMapping("/jobs/assignments/eod")
    public ResponseEntity<String> runEodAssignmentNow() {
        // NOTE: In real project, secure this endpoint for ADMIN/SUPERADMIN only.
        assignmentService.assignSubmittedIdeasEndOfDay();
        return ResponseEntity.accepted().body("EOD assignment triggered");
    }
    
    

    @GetMapping("/ideas/{ideaId}")
    public ResponseEntity<ProgressionDTO> getIdeaProgression(@PathVariable Integer ideaId) {
        return ResponseEntity.ok(progressionService.build(ideaId));
    }

        
    // Pageable helper
    // sort=createdAt,asc OR createdAt,desc

    private Pageable toPageable(int page, int size, String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        String[] parts = sortParam.split(",");
        String field = parts[0].trim();

        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(dir, field));
    }
}
