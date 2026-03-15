package com.ideatrack.main.exception;

public class ObjectiveProofPathEmptyException extends RuntimeException{

    public ObjectiveProofPathEmptyException(Integer objectiveSeq) {
        super("Stored proof path cannot be empty. objectiveSeq=" + objectiveSeq);
    }
}