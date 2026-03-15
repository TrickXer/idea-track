package com.ideatrack.main.exception;

public class ObjectiveSeqDuplicateException extends RuntimeException{

    public ObjectiveSeqDuplicateException(Integer seq) {
        super("Duplicate objectiveSeq in request payload: " + seq);
    }
}