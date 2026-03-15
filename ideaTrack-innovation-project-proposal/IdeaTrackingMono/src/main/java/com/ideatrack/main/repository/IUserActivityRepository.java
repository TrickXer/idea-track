package com.ideatrack.main.repository;

import com.ideatrack.main.data.UserActivity;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Constants.ActivityType;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IUserActivityRepository extends JpaRepository<UserActivity, Integer> {
    // Used in GamificationService
    // Returns all interactions (comments, votes, saves, etc.) for the user in reverse chronological order.
    List<UserActivity> findByUserOrderByCreatedAtDesc(User user);
    // Counts comments and votes made by the user; contributes to universal/role-specific badge unlocks.
    int countByUserAndActivityType(User user, Constants.ActivityType activityType);

    //Used in Analytics Service Module
    long countByUser_UserIdAndActivityTypeAndCreatedAtBetween(Integer userId, Constants.ActivityType activityType, LocalDateTime start, LocalDateTime end);

    // Used in GamificationService
    // Returns only XP-impacting activities (non-zero delta) in reverse chronological order for the XP history view.
    List<UserActivity> findByUserAndDeltaNotOrderByCreatedAtDesc(User user, int delta);

    List<UserActivity> findByUserAndIdeaOrderByCreatedAtDesc(User user, Idea idea);

    //Used in Analytics Service Module
    long countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(Integer userId,
          Constants.ActivityType reviewdiscussion, Constants.IdeaStatus accepted, LocalDateTime start, LocalDateTime end);

    //Used in UserActivity
    //findAllByIdea_IdeaIdAndActivityTypeAndDeletedFalseOrderByCreatedAtAsc
    List<UserActivity> findAllByIdea_IdeaIdAndActivityTypeAndDeletedFalseOrderByCreatedAtAsc(
            Integer ideaId, Constants.ActivityType type);

    List<UserActivity> findAllByReplyParent_UserActivityIdAndDeletedFalseOrderByCreatedAtAsc(Integer parentId);

    //Used in IdeaService, UserActivity
    //findFirstByUser_UserIdAndIdea_IdeaIdAndActivityTypeAndDeletedFalse
    //"Interaction" as in for vote/save/comment
    Optional<UserActivity> findFirstByIdea_IdeaIdAndActivityTypeAndDeletedFalse(
    	    Integer ideaId, Constants.ActivityType type);
    
    //Used in USerActivity
    //findFirstByUser_UserIdAndIdea_IdeaIdAndActivityType
    //"Any" implies don't care about the deleted flag
    Optional<UserActivity> findFirstByUser_UserIdAndIdea_IdeaIdAndActivityType(
            Integer userId, Integer ideaId, Constants.ActivityType activityType);
    
    //Used in IdeaService
    //countByIdea_IdeaIdAndActivityTypeAndVoteTypeAndDeletedFalse
    long countByIdea_IdeaIdAndActivityTypeAndVoteTypeAndDeletedFalse(
            Integer ideaId, Constants.ActivityType type, Constants.VoteType voteType);

    // Used in IdeaService
    //countByIdea_IdeaIdAndActivityTypeAndDeletedFalse
    long countByIdea_IdeaIdAndActivityTypeAndDeletedFalse(Integer ideaId, Constants.ActivityType type);


    // Used in HierarchyService
    // Finds the latest stage-scoped structured decision (not deleted) made by a reviewer on this idea; used to populate node’s decision and decisionAt.
    UserActivity findFirstByUser_UserIdAndIdea_IdeaIdAndStageIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(
            Integer reviewerId,
            Integer ideaId,
            Integer stageId
    );

    // If the stage-scoped decision is absent, fetch the latest idea-scoped structured decision for that reviewer; also used to fetch the admin decision (and timestamp) for the timeline/admin block.
    UserActivity findFirstByUser_UserIdAndIdea_IdeaIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(
            Integer reviewerId,
            Integer ideaId
    );
    long countByUser_Department_DeptIdAndActivityTypeAndCreatedAtBetween(Integer deptId, Constants.ActivityType activityType, LocalDateTime start, LocalDateTime end);
    // Used in GamificationService
    // Retrieves interactions filtered by the specified type (COMMENT/VOTE/SAVE) in reverse chronological order.
    List<UserActivity> findByUserAndActivityTypeOrderByCreatedAtDesc(User user, Constants.ActivityType activityType);


    // Returns a Slice of all activities for the given user, ordered by newest first.
    // Used for infinite-scroll activity feeds without running COUNT(*) queries.
    // Ideal for large datasets where only content + hasNext are needed.
    Slice<UserActivity> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Retrieves a Slice of only XP‑changing activities (delta != 0), newest-first.
    // Useful for XP history screens where you scroll through past XP events.
    // Avoids heavy count operations while still supporting hasNext pagination.
    Slice<UserActivity> findByUserAndDeltaNotOrderByCreatedAtDesc(User user, int delta, Pageable pageable);

    // Fetches a Slice of activities filtered by ActivityType (COMMENT, VOTE, SAVE).
    // Designed for category-specific feeds with infinite scrolling behavior.
    // Returns page-sized chunks along with a hasNext flag for seamless pagination.
    Slice<UserActivity> findByUserAndActivityTypeOrderByCreatedAtDesc(
            User user,
            Constants.ActivityType activityType,
            Pageable pageable
    );
    // =========================================================================
    // REVIEWER MODULE
    // =========================================================================

    boolean existsByIdea_IdeaIdAndActivityTypeAndEvent(Integer ideaId, Constants.ActivityType type, String event);

    boolean existsByIdea_IdeaIdAndEvent(Integer ideaId, String event);

    List<UserActivity> findByIdea_IdeaIdAndStageIdAndActivityTypeAndDeletedFalseOrderByCreatedAtAsc(
            Integer ideaId, Integer stageId, Constants.ActivityType activityType);

    List<UserActivity> findAllByIdea_IdeaId(Integer ideaId);

    List<UserActivity> findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalseOrderByCreatedAtAsc(
            Integer ideaId, Constants.ActivityType activityType, Integer stageId);
    
    Optional<UserActivity> findFirstByIdea_IdeaIdAndUser_UserIdAndActivityTypeAndDeletedFalse(Integer ideaId, Integer userId,
    		ActivityType vote);
    
 // ==========================================================
 // ✅ Added for Reviewer Module: pagination & safer refine checks
 // (APPEND-ONLY: do not remove existing methods)
 // ==========================================================

     /**
      * Safer refine check: event + activityType + deletedFalse.
      * Prevents unrelated "REFINE_ACTION" rows (if any) from affecting logic.
      */
     boolean existsByIdea_IdeaIdAndActivityTypeAndEventAndDeletedFalse(
             Integer ideaId, Constants.ActivityType type, String event
     );

     /**
      * Paged stage discussion fetch (non-deleted).
      * Keeps existing list method untouched for backward compatibility.
      */
     Page<UserActivity> findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalse(
             Integer ideaId, Constants.ActivityType activityType, Integer stageId,
             org.springframework.data.domain.Pageable pageable
     );
  // ==========================================================
  // ✅ Added for Reviewer Workflow timeline + SLA 
  // ==========================================================

  /**
   * Refine-used check. Uses CURRENTSTATUS timeline rows to mark refine usage.
   */
  boolean existsByIdea_IdeaIdAndActivityTypeAndDecisionAndDeletedFalse(
          Integer ideaId, Constants.ActivityType activityType, Constants.IdeaStatus decision
  );

  /**
   * Targeted refine-outcome check: only looks at MATRIX_OUTCOME / MATRIX_OUTCOME_SLA events
   * so individual reviewer REVIEWER_DECISION rows don't falsely mark refineUsed=true.
   */
  boolean existsByIdea_IdeaIdAndActivityTypeAndDecisionAndEventInAndDeletedFalse(
          Integer ideaId,
          Constants.ActivityType activityType,
          Constants.IdeaStatus decision,
          java.util.Collection<String> events
  );

  /**
   * Get earliest stage-start timeline row (used to compute 3-day SLA window).
   */
  Optional<UserActivity> findFirstByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalseOrderByCreatedAtAsc(
          Integer ideaId, Integer stageId, Constants.ActivityType activityType, String event
  );

  /**
   * Find all CURRENTSTATUS rows for this stage and event (for auditing).
   */
  List<UserActivity> findAllByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalseOrderByCreatedAtAsc(
          Integer ideaId, Integer stageId, Constants.ActivityType activityType, String event
  );
  boolean existsByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalse(Integer ideaId, Integer stage,
       ActivityType currentstatus, String eventStageStart);
  
  
  @Query("""
		   select (count(ua) > 0)
		   from UserActivity ua
		   where ua.deleted = false
		     and ua.activityType = :type
		     and ua.event = :event
		     and ua.decision = :decision
		     and ua.idea.deleted = false
		     and ua.idea.ideaStatus = :ideaStatus
		""")
		boolean existsTriggerForCurrentlySubmittedIdeas(
		 @Param("type") com.ideatrack.main.data.Constants.ActivityType type,
		 @Param("event") String event,
		 @Param("decision") Constants.IdeaStatus decision,
		 @Param("ideaStatus")Constants.IdeaStatus ideaStatus
		);
  

}