package com.ideatrack.main.exception;

public class IdeaStatusNotAcceptedException extends RuntimeException{

    public IdeaStatusNotAcceptedException(Integer ideaId) {
        super("Idea must be ACCEPTED to convert to a proposal. Idea ID: " + ideaId);
    }
}
