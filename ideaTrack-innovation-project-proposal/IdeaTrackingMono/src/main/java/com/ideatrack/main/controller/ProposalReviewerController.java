package com.ideatrack.main.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ideatrack.main.dto.objective.ObjectivesResponse;
import com.ideatrack.main.dto.reviewer.ProposalDecisionRequest;
import com.ideatrack.main.service.ObjectiveService;
import com.ideatrack.main.service.ProposalReviewService;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

//PROPOSAL REVIEW

//Done by Vibhuti

@RestController
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
@RequestMapping("/api/adminReview")
@RequiredArgsConstructor
public class ProposalReviewerController {

	private final ProposalReviewService proposalReviewService;

	private final ObjectiveService objectiveService;

		/*
		 * 1) When reviewer is assigned, the proposal will go into the UNDERREVIEW stage. 
		 * http://localhost:8091/api/adminReview/proposal/1/start
		 */
		@PostMapping("/proposal/{proposalId}/start")
		public ResponseEntity<String> startProposalReview(@PathVariable Integer proposalId) {
			proposalReviewService.startProposalReview(proposalId);
			return ResponseEntity.ok("Proposal review started (UNDERREVIEW:PROPOSAL).");
		}

		/*
		 * 2) getting proposal for review
		 * http://localhost:8091/api/adminReview/proposal/1/review
		 */		
		@GetMapping("/proposal/{proposalId}/review")
		public Page<ObjectivesResponse> getReviewObjectives(
				@PathVariable Integer proposalId,
				@RequestParam(required = false) Boolean hasProof,
				@RequestParam(required = false) String proofType,
				@RequestParam(required = false) Boolean mandatory,
				@RequestParam(required = false, defaultValue = "") String search,
				@RequestParam(defaultValue = "1") @Min(1) int page,
				@RequestParam(defaultValue = "20") @PositiveOrZero int pageSize,
				@RequestParam(defaultValue = "objectiveSeq,asc") String sort) {

			Pageable pageable = toPageable(page, pageSize, sort);
			return objectiveService.getForReview(proposalId, hasProof, proofType, mandatory, search, pageable);
		}
		
		/*
		 * 3) Proposal decision: UNDERREVIEW(PROPOSAL) -> APPROVED or REJECTED
		 * http://localhost:8091/api/adminReview/proposal/1/decision
		 */		
		@PostMapping("/proposal/{proposalId}/decision")
		public ResponseEntity<String> processProposalDecision(
				@PathVariable Integer proposalId,
				@RequestBody ProposalDecisionRequest request) {
			proposalReviewService.processDecision(proposalId, request);
			return ResponseEntity.ok("Proposal decision processed.");
		}

	private Pageable toPageable(int page, int size, String sort) {
		String sortBy = "createdAt";
		Sort.Direction direction = Sort.Direction.DESC;

		if (sort != null && !sort.isBlank()) {
			String[] parts = sort.split(",");
			sortBy = parts[0].trim();
			if (parts.length > 1) {
				String dir = parts[1].trim();
				direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
			}
		}

		int zeroBased = Math.max(0, page - 1);

		return PageRequest.of(zeroBased, size, Sort.by(direction, sortBy));
	}

}