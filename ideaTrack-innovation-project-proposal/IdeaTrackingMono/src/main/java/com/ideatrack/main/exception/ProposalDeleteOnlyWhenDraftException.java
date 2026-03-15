package com.ideatrack.main.exception;

import com.ideatrack.main.data.Constants.IdeaStatus;

public class ProposalDeleteOnlyWhenDraftException extends RuntimeException {
    public ProposalDeleteOnlyWhenDraftException(Integer proposalId, IdeaStatus currentStatus) {
        super("Only Draft proposals can be deleted. proposalId=" + proposalId + ", status=" + currentStatus);
    }
}