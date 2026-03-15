package com.ideatrack.main.exception;

import com.ideatrack.main.data.Constants.IdeaStatus;

public class ProposalSubmitOnlyWhenDraftException  extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Integer proposalId;
    private final IdeaStatus currentStatus;

    public ProposalSubmitOnlyWhenDraftException(Integer proposalId, IdeaStatus currentStatus) {
        super("Only Draft proposals can be submitted. proposalId=" + proposalId + ", status=" + currentStatus);
        this.proposalId = proposalId;
        this.currentStatus = currentStatus;
    }

    public Integer getProposalId() {
        return proposalId;
    }

    public IdeaStatus getCurrentStatus() {
        return currentStatus;
    }
}