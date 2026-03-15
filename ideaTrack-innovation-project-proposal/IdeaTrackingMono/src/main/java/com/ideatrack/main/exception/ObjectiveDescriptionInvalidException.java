package com.ideatrack.main.exception;

public class ObjectiveDescriptionInvalidException extends RuntimeException{

    public ObjectiveDescriptionInvalidException(Integer seq) {
        super("description is required (≤ 2000 chars). objectiveSeq=" + seq);
    }
}