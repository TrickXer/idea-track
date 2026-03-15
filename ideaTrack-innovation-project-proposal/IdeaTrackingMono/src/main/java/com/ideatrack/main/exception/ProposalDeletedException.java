package com.ideatrack.main.exception;

public class ProposalDeletedException extends RuntimeException {
    public ProposalDeletedException(Integer proposalId) {
        super("Proposal is deleted. proposalId=" + proposalId);
    }
}
