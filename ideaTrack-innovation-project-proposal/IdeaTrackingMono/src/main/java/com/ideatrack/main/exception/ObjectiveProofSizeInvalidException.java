package com.ideatrack.main.exception;

public class ObjectiveProofSizeInvalidException extends RuntimeException{

    public ObjectiveProofSizeInvalidException(Integer objectiveSeq, Long size, long max) {
        super("Proof size must be > 0 and ≤ " + (max / (1024 * 1024)) + " MB. "
              + "objectiveSeq=" + objectiveSeq + ", size=" + size);
    }
}