package com.ideatrack.main.repository;

import com.ideatrack.main.data.AssignedReviewerToIdea;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IAssignedReviewerToIdeaRepository extends JpaRepository<AssignedReviewerToIdea, Integer> {
    
    // Strict Match (Idea + Reviewer + Stage)
    Optional<AssignedReviewerToIdea> findByIdea_IdeaIdAndReviewer_UserIdAndStage(
        Integer ideaId, Integer userId, Integer stage);
    
    Optional <AssignedReviewerToIdea> findByIdea_IdeaId(Integer ideaId);

	long countByReviewer_UserIdAndDeletedFalseAndCreatedAtBetween(Integer userId, LocalDateTime start, LocalDateTime end);
	
	@Query("""
			  SELECT COUNT(ar)
			  FROM AssignedReviewerToIdea ar
			  WHERE ar.reviewer.userId = :userId
			    AND ar.deleted = false
			    AND ar.createdAt >= :start AND ar.createdAt < :end
			    AND ar.updatedAt IS NOT NULL
			    AND ar.updatedAt <= function('timestampadd', DAY, 3, ar.createdAt)
			""")
	long countOnTimeByUserAndMonthWithin3Days(
			    @Param("userId") Integer userId,
			    @Param("start") LocalDateTime start,
			    @Param("end") LocalDateTime end
				);

	@Modifying
	@Transactional
    int deleteByIdea_IdeaIdAndStageAndReviewer_UserIdIn(Integer ideaId,
                                                         Integer stage,
                                                         List<Integer> reviewerIds);

	    @Query("""
	        SELECT r FROM AssignedReviewerToIdea r
	         WHERE r.idea.ideaId = :ideaId
	           AND r.stage = :stage
	           AND r.reviewer.userId IN :reviewerIds
	           AND r.deleted = false
	    """)
	    List<AssignedReviewerToIdea> findAllForRemoval(@Param("ideaId") Integer ideaId,
	                                                   @Param("stage") Integer stage,
	                                                   @Param("reviewerIds") List<Integer> reviewerIds);

	    // ✅ Bulk UPDATE soft delete (recommended):
	    @Modifying(clearAutomatically = true, flushAutomatically = true)
	    @Transactional
	    @Query("""
	        UPDATE AssignedReviewerToIdea r
	           SET r.deleted = true
	  	         WHERE r.idea.ideaId = :ideaId
	           AND r.stage = :stage
	           AND r.reviewer.userId IN :reviewerIds
	           AND r.deleted = false
	    """)


	    int softDeleteAssignments(@Param("ideaId") Integer ideaId,
	                              @Param("stage") Integer stage,
	                              @Param("reviewerIds") List<Integer> reviewerIds,
	                              @Param("now") OffsetDateTime now);


    // ****** REVIEWER MODULE ******

    @Query("SELECT a FROM AssignedReviewerToIdea a " +
            "WHERE a.reviewer.userId = :reviewerId " +
            "AND (:decision IS NULL OR a.decision = :decision) " +
            "AND a.deleted = false")
     List<AssignedReviewerToIdea> findDashboardIdeas(
             @Param("reviewerId") Integer reviewerId,
             @Param("decision") String decision);

 // ==========================================================
 // ✅ Added for Reviewer Module pagination & strict validations
 // ==========================================================

     /**
      * Strict assignment lookup with soft-delete safeguard.
      * Prevents stage-mismatch loophole.
      */
     Optional<AssignedReviewerToIdea> findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
             Integer ideaId, Integer userId, Integer stage
     );

     /**
      * Fetch assignments only for a given idea & stage (non-deleted).
      * Used for "all approved in stage" checks.
      */
     List<AssignedReviewerToIdea> findByIdea_IdeaIdAndStageAndDeletedFalse(Integer ideaId, Integer stage);

     /**
      * Fetch all assignments for an idea in stage order (non-deleted).
      * Used to compute next stage reliably (avoids stageCount off-by-one issues).
      */
     List<AssignedReviewerToIdea> findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(Integer ideaId);

     /**
      * Dashboard ideas with pagination (non-deleted).
      * NOTE: Uses existing decision column filter.
      */
     @Query(
         value = "SELECT a FROM AssignedReviewerToIdea a " +
                 "WHERE a.reviewer.userId = :reviewerId " +
                 "AND (:decision IS NULL OR UPPER(a.decision) = :decision) " +
                 "AND a.deleted = false",
         countQuery = "SELECT COUNT(a) FROM AssignedReviewerToIdea a " +
                 "WHERE a.reviewer.userId = :reviewerId " +
                 "AND (:decision IS NULL OR UPPER(a.decision) = :decision) " +
                 "AND a.deleted = false"
     )
     Page<AssignedReviewerToIdea> findDashboardIdeasPaged(
             @Param("reviewerId") Integer reviewerId,
             @Param("decision") String decision,
             org.springframework.data.domain.Pageable pageable
     );

     /**
      * Decision history for an idea from assignment table (non-deleted).
      * This avoids relying on UserActivity decision logging (which other modules may read).
      */
     Page<AssignedReviewerToIdea> findByIdea_IdeaIdAndDeletedFalseAndDecisionIsNotNull(
             Integer ideaId,
             org.springframework.data.domain.Pageable pageable
     );
  // ==========================================================
  // ✅ Added for Reviewer Module security checks (APPEND-ONLY)
  // ==========================================================
  /**
   * Fast authorization check for reviewer discussion/decision.
   */
  boolean existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
          Integer ideaId, Integer userId, Integer stage
  );

  boolean existsByIdea_IdeaIdAndReviewer_UserIdAndStage(Integer ideaId, Integer userId, Integer stage);

  /**
   * Resets all reviewer decisions/feedback for a given idea+stage back to null
   * so that reviewers can vote again after an owner resubmits from REFINE state.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("""
      UPDATE AssignedReviewerToIdea a
         SET a.decision = null,
             a.feedback = null
       WHERE a.idea.ideaId = :ideaId
         AND a.stage = :stage
         AND a.deleted = false
      """)
  void resetDecisionsForStage(@Param("ideaId") Integer ideaId, @Param("stage") Integer stage);

  List<AssignedReviewerToIdea> findAllByIdea_IdeaIdAndFeedbackIsNotNullOrderByUpdatedAtDesc(Integer ideaId);
	// Used in HierarchyService
	//Fetches all reviewer assignments linked to the idea, sorted by stage. Each row is transformed into a HierarchyNodeDTO (reviewer identity/public info + assignment details + derived decision).
    List<AssignedReviewerToIdea> findByIdea_IdeaIdOrderByStageAsc(Integer ideaId);
}
