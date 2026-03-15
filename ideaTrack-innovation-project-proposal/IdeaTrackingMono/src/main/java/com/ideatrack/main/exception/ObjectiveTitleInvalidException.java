package com.ideatrack.main.exception;

public class ObjectiveTitleInvalidException extends RuntimeException{

    public ObjectiveTitleInvalidException(Integer seq) {
        super("title is required (≤ 150 chars). objectiveSeq=" + seq);
    }
}