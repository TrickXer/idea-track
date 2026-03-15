package com.ideatrack.main.exception;

public class ProofFileRequiredException  extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProofFileRequiredException() {
        super("File required.");
    }
}