package com.ideatrack.main.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ideatrack.main.data.ReviewerCategory;
import com.ideatrack.main.service.ReviewerCategoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reviewer-categories")
public class ReviewerCategoryController {

    private final ReviewerCategoryService service;

    // POST /api/v1/admin/reviewer-categories
    @PostMapping
    public ResponseEntity<ReviewerCategoryResponse> assign(@Valid @RequestBody ReviewerCategoryCreateRequest req) {
        ReviewerCategory rc = service.assign(req.reviewerId(), req.categoryId(), req.stageId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewerCategoryResponse.from(rc));
    }

    // GET /api/v1/admin/reviewer-categories/{categoryId}
    @GetMapping("/{categoryId}")
    public List<ReviewerCategoryResponse> list(@PathVariable Integer categoryId) {
        return service.listByCategory(categoryId)
                .stream().map(ReviewerCategoryResponse::from).toList();
    }

    // PUT /api/v1/admin/reviewer-categories/{reviewerCategoryId}
    @PutMapping("/{reviewerCategoryId}")
    public ReviewerCategoryResponse update(@PathVariable Integer reviewerCategoryId,
                                           @Valid @RequestBody ReviewerCategoryUpdateRequest req) {
        ReviewerCategory rc = service.updateStage(reviewerCategoryId, req.stageId());
        return ReviewerCategoryResponse.from(rc);
    }

    // DELETE /api/v1/admin/reviewer-categories/{reviewerCategoryId}
    @DeleteMapping("/{reviewerCategoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer reviewerCategoryId) {
        service.softDelete(reviewerCategoryId);
    }

    // ---- DTOs (inline for brevity) ----

    public record ReviewerCategoryCreateRequest(
            @NotNull Integer reviewerId,
            @NotNull Integer categoryId,
            @NotNull Integer stageId
    ) {}

    public record ReviewerCategoryUpdateRequest(@NotNull Integer stageId) {}

    public record ReviewerCategoryResponse(
            Integer reviewerCategoryId,
            Integer reviewerId,
            String reviewerName,
            Integer categoryId,
            String categoryName,
            Integer stageId
    ) {
        public static ReviewerCategoryResponse from(ReviewerCategory rc) {
            return new ReviewerCategoryResponse(
                    rc.getReviewerCategoryId(),
                    rc.getReviewer().getUserId(),
                    // adjust per your User entity
                    rc.getReviewer().getName(),
                    rc.getCategory().getCategoryId(),
                    // adjust per your Category entity
                    rc.getCategory().getName(),
                    rc.getAssignedStageId()
            );
        }
    }
}
