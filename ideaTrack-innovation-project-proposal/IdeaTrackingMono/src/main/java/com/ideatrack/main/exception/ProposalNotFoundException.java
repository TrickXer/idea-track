package com.ideatrack.main.exception;

public class ProposalNotFoundException extends RuntimeException {
    public ProposalNotFoundException(Integer proposalId) {
        super("Proposal not found: " + proposalId);
    }
}