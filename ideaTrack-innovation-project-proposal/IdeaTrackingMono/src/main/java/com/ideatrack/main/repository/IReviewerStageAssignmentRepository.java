package com.ideatrack.main.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ideatrack.main.data.ReviewerCategory;

@Repository
public interface IReviewerStageAssignmentRepository extends JpaRepository<ReviewerCategory, Integer> {

	
//	Used in ReviewerStageAssignment Module, returns list of assigned reviewers. - Advait.
	List<ReviewerCategory> findByDeletedFalse();

//	Used in ReviewerStageAssignment Module, returns the reviewer for the given reviewerId, categoryId and stageNo. - Advait.
	Optional<ReviewerCategory> findByReviewer_UserIdAndCategory_CategoryIdAndAssignedStageIdAndDeletedFalse(Integer reviewerId, Integer categoryId, Integer stageNo);

//	Used in ReviewerStageAssignment Module, checks if the reviewers is already available or not. - Advait.
	boolean existsByReviewer_UserIdAndDeletedFalse(Integer reviewerId);

}
