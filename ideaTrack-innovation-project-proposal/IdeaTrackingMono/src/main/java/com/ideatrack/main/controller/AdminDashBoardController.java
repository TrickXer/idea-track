package com.ideatrack.main.controller;
import com.ideatrack.main.dto.admin.AdminHealthSummaryDto;
import com.ideatrack.main.dto.admin.AdminOverdueReviewerDto;
import com.ideatrack.main.dto.admin.AdminUpdateUserStatusRequest;
import com.ideatrack.main.service.AdminDashboardService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

//Done by vibhuti

@RestController
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class AdminDashBoardController {

	    private final AdminDashboardService adminService;
	    
		/*
		 * 1) will list all the proposals for the admin to review
		 * http://localhost:8091/api/admin/proposals
		 */	   
	    @GetMapping("/proposals")
	    public ResponseEntity<Object> listPendingOrStuckProposals(
	            @RequestParam(name = "status", required = false) String status,
	            @RequestParam(name = "stageId", required = false) Long stageId,
	            @RequestParam(name = "reviewerId", required = false) Long reviewerId,
	            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
	            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size,
	            @RequestParam(name = "sort", required = false) String sort
	    ) {
	        var pageable = buildPageable(page, size, sort);
	        var result = adminService.listPendingOrStuckProposals(status, stageId, reviewerId, pageable);
	        return ResponseEntity.ok(result);
	    }

		/*
		 * 2) will see reviewers with overdue task
		 * http://localhost:8091/api/admin/reviewers/overdue
		 */	   
	    @GetMapping("/reviewers/overdue")
	    public ResponseEntity<Page<AdminOverdueReviewerDto>> getOverdueReviewers(
	            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
	            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size,
	            @RequestParam(name = "sort", required = false) String sort
	    ) {
	        var pageable = buildPageable(page, size, sort);
	        return ResponseEntity.ok(adminService.getOverdueReviewers(pageable));
	    }

		/*
		 * 3) will check the health of the application
		 * http://localhost:8091/api/admin/health/summary
		 */	    
	    @GetMapping("/health/summary")
	    public ResponseEntity<AdminHealthSummaryDto> getHealthSummary() {
	        return ResponseEntity.ok(adminService.getHealthSummary());
	    }

		/*
		 * 4) can inactivate or activate the user
		 * http://localhost:/8091/api/admin/users/1/status
		 */	   
		 @PatchMapping(value = "/users/{userId}/status", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<java.util.Map<String, String>> updateUserStatus(
	            @PathVariable("userId") @NotNull Integer userId,
	            @RequestBody @Valid AdminUpdateUserStatusRequest request
	    ) {
	        adminService.updateUserStatus(userId, request);
	        return ResponseEntity.ok(java.util.Map.of("message", "User status updated"));
	    }

	    private PageRequest buildPageable(int page, int size, String sortParam) {
	        if (sortParam == null || sortParam.isBlank()) {
	            return PageRequest.of(page, size);
	        }
	        String[] parts = sortParam.split(",", 2);
	        String field = parts[0].trim();
	        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
	                ? Sort.Direction.DESC : Sort.Direction.ASC;
	        return PageRequest.of(page, size, Sort.by(dir, field));
	    }
	}