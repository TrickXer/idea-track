package com.ideatrack.main.exception;

public class ObjectiveNotFoundInProposalException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Integer proposalId;
    private final Integer objectiveId;

    public ObjectiveNotFoundInProposalException(Integer proposalId, Integer objectiveId) {
        super("Objective not found in proposal. proposalId=" + proposalId + ", objectiveId=" + objectiveId);
        this.proposalId = proposalId;
        this.objectiveId = objectiveId;
    }

    public Integer getProposalId() {
        return proposalId;
    }

    public Integer getObjectiveId() {
        return objectiveId;
    }
}
