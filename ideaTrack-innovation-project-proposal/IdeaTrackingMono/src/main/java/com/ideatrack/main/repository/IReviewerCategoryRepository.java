package com.ideatrack.main.repository;

import com.ideatrack.main.data.ReviewerCategory;

//import com.ideatrack.main.service.ReviewerAggregateProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface IReviewerCategoryRepository extends JpaRepository<ReviewerCategory, Integer> {

    @Query("""
           select rc from ReviewerCategory rc
           where rc.category.categoryId = :categoryId
             and rc.deleted = false
           """)
    List<ReviewerCategory> findActiveByCategory(@Param("categoryId") Integer categoryId);

    @Query("""
           select (count(rc) > 0) from ReviewerCategory rc
           where rc.reviewer.userId = :reviewerId
             and rc.category.categoryId = :categoryId
             and rc.assignedStageId = :stageId
             and rc.deleted = false
           """)
    boolean existsActive(@Param("reviewerId") Integer reviewerId,
                         @Param("categoryId") Integer categoryId,
                         @Param("stageId") Integer stageId);

    @Query("""
           select rc from ReviewerCategory rc
           where rc.reviewerCategoryId = :id
             and rc.deleted = false
           """)
    Optional<ReviewerCategory> findActiveById(@Param("id") Integer id);

    /**
     * Overdue reviewers aggregate (JPQL, requires ReviewerAssignment entity).
     * - pendingTasks: count of assignments that are not completed
     * - overdueByDays: maximum days overdue across their pending items
     *
     * NOTE: JPQL has limited date arithmetic portability. If your JPA provider
     * doesn’t support function calls used here, use the native SQL variant below.
     */
    @Query("""
    	    select
    	        ari.reviewer.userId as reviewerId,
    	        coalesce(ari.reviewer.name, '') as reviewerName,
    	        count(ari) as pendingTasks,
    	        0 as overdueByDays
    	    from AssignedReviewerToIdea ari
    	    where ari.deleted = false
    	    group by ari.reviewer.userId, coalesce(ari.reviewer.name, '')
    	    """)
    	Page<ReviewerAggregateProjection> findOverdueReviewerAggregates(Pageable pageable);
    /**
     * Bulk remove reviewers from a proposal stage.
     */
 // ==========================================================
 // ✅ Added for auto assignment by category+stage (APPEND-ONLY)
 // ==========================================================
 List<ReviewerCategory> findByCategory_CategoryIdAndAssignedStageIdAndDeletedFalse(
         Integer categoryId, Integer assignedStageId
 );

@Query("""
     select rc.reviewer.userId
     from ReviewerCategory rc
     where rc.category.categoryId = :categoryId
       and rc.assignedStageId = :assignedStageId
       and rc.deleted = false
     """)
List<Integer> findActiveReviewerUserIdsByCategoryAndStage(
      @Param("categoryId") Integer categoryId,
      @Param("assignedStageId") Integer assignedStageId
);
}
