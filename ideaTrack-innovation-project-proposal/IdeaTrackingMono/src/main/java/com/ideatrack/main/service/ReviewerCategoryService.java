package com.ideatrack.main.service;

import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.ReviewerCategory;
import com.ideatrack.main.data.User;
import com.ideatrack.main.repository.ICategoryRepository;
import com.ideatrack.main.repository.IReviewerCategoryRepository;
import com.ideatrack.main.repository.IUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewerCategoryService {

    private final IReviewerCategoryRepository repo;
    private final IUserRepository userRepo;
    private final ICategoryRepository categoryRepo;

    @Transactional
    public ReviewerCategory assign(Integer reviewerId, Integer categoryId, Integer stageId) {
        if (repo.existsActive(reviewerId, categoryId, stageId)) {
            throw new IllegalStateException("Mapping already exists for reviewer/category/stage");
        }

        User reviewer = userRepo.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerId));

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        ReviewerCategory rc = ReviewerCategory.builder()
                .reviewer(reviewer)
                .category(category)
                .assignedStageId(stageId)
                .build();

        return repo.save(rc);
    }


@Transactional(Transactional.TxType.SUPPORTS)
    public List<ReviewerCategory> listByCategory(Integer categoryId) {
        return repo.findActiveByCategory(categoryId);
    }

    @Transactional
    public ReviewerCategory updateStage(Integer reviewerCategoryId, Integer newStageId) {
        ReviewerCategory rc = repo.findActiveById(reviewerCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found or deleted: " + reviewerCategoryId));

        rc.setAssignedStageId(newStageId);
        return repo.save(rc);
    }

    @Transactional
    public void softDelete(Integer reviewerCategoryId) {
        ReviewerCategory rc = repo.findActiveById(reviewerCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found or already deleted: " + reviewerCategoryId));

        rc.setDeleted(true);
        repo.save(rc);
    }
}
