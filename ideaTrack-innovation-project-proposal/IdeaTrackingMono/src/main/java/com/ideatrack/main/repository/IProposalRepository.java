package com.ideatrack.main.repository;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.data.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IProposalRepository extends JpaRepository<Proposal, Integer>, JpaSpecificationExecutor<Proposal> { 
    // Used in GamificationService
    // Counts proposals authored by the user.
    int countByUser(User user);

    boolean existsByIdea_IdeaIdAndDeletedFalse(Integer ideaId);

    Optional<Proposal> findByProposalIdAndDeletedFalse(Integer proposalId);

    @Query("select p from Proposal p left join fetch p.objectives where p.proposalId = :id and p.deleted = false")
    Optional<Proposal> findByIdWithObjectives(@Param("id") Integer id);
    
    Page<Proposal> findByUser_UserId(Integer userId, Pageable pageable);
    
	List<Proposal> findByIdea_IdeaIdInAndUser_UserIdAndDeletedFalse(
	        java.util.Collection<Integer> ideaIds, Integer userId);
    
    boolean existsByProposalIdAndDeletedFalseAndIdeaStatus(Integer proposalId, Constants.IdeaStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Proposal p where p.proposalId = :proposalId and p.deleted = false")
    Optional<Proposal> findForUpdate(@Param("proposalId") Integer proposalId);

    /**
     * Admin list with optional filters:
     * - status: Proposal.ideaStatus (enum)
     * - stageId: matches AssignedReviewerToIdea.stage (int) for the same idea
     * - reviewerId: matches AssignedReviewerToIdea.reviewer.userId for the same idea
     *
     */
    @Query("""
        select p
        from Proposal p
        where p.deleted = false
          and (:status is null or p.ideaStatus = :status)
          and (:stageId is null or exists (
                select 1 from AssignedReviewerToIdea ari
                where ari.deleted = false
                  and ari.idea = p.idea
                  and ari.stage = :stageId
          ))
          and (:reviewerId is null or exists (
                select 1 from AssignedReviewerToIdea ari2
                where ari2.deleted = false
                  and ari2.idea = p.idea
                  and ari2.reviewer.userId = :reviewerId
          ))
        """)
    Page<Proposal> searchAdminList(
            @Param("status") Constants.IdeaStatus status,
            @Param("stageId") Integer stageId,
            @Param("reviewerId") Integer reviewerId,
            Pageable pageable
    );

}
