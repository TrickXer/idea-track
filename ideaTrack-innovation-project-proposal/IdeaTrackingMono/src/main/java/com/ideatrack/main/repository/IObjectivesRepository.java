package com.ideatrack.main.repository;

import com.ideatrack.main.data.Objectives;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface IObjectivesRepository extends
        JpaRepository<Objectives, Long>,
        JpaSpecificationExecutor<Objectives> {

    Optional<Objectives> findByIdAndProposal_ProposalId(Long id, Integer proposalId);
    boolean existsByIdAndProposal_ProposalId(Long id, Integer proposalId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByIdAndProposal_ProposalId(Long id, Integer proposalId);

    List<Objectives> findAllByProposal_ProposalIdAndProposal_DeletedFalseOrderByObjectiveSeqAsc(Integer proposalId);

    long countByProposal_ProposalId(Integer proposalId);
    long countByProposal_ProposalIdAndMandatoryTrue(Integer proposalId);

    boolean existsByProposal_ProposalIdAndObjectiveSeq(Integer proposalId, Integer objectiveSeq);

    @Query("select coalesce(max(o.objectiveSeq), 0) from Objectives o where o.proposal.proposalId = :proposalId")
    int findMaxSeqByProposalId(@Param("proposalId") Integer proposalId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update Objectives o
              set o.proofFileName = :fileName,
                  o.proofFilePath = :filePath,
                  o.proofContentType = :contentType,
                  o.proofSizeBytes = :sizeBytes
            where o.proposal.proposalId = :proposalId
              and o.objectiveSeq = :objectiveSeq
           """)
    int updateProofForObjective(@Param("proposalId") Integer proposalId,
                                @Param("objectiveSeq") Integer objectiveSeq,
                                @Param("fileName") String fileName,
                                @Param("filePath") String filePath,
                                @Param("contentType") String contentType,
                                @Param("sizeBytes") Long sizeBytes);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByProposal_ProposalIdAndObjectiveSeq(Integer proposalId, Integer objectiveSeq);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByProposal_ProposalId(Integer proposalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Objectives> findWithLockById(Long id);
}