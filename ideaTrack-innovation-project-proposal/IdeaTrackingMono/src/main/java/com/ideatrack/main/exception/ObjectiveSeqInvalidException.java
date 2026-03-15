package com.ideatrack.main.exception;

public class ObjectiveSeqInvalidException extends RuntimeException{

    public ObjectiveSeqInvalidException(Integer seq) {
        super("objectiveSeq must be ≥ 1. Provided: " + seq);
    }
}