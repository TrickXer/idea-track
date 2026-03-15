package com.ideatrack.main.exception;

public class ObjectiveProofMissingException extends RuntimeException{

    public ObjectiveProofMissingException(Integer objectiveSeq) {
        super("Each objective must include a 'proof' at creation. objectiveSeq=" + objectiveSeq);
    }
}