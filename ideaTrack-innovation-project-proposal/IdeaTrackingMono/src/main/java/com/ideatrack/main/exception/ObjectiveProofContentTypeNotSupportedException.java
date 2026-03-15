package com.ideatrack.main.exception;

public class ObjectiveProofContentTypeNotSupportedException extends RuntimeException{

    public ObjectiveProofContentTypeNotSupportedException(Integer objectiveSeq, String contentType) {
        super("Proof must be PDF (application/pdf) or JPG (image/jpeg). objectiveSeq=" 
              + objectiveSeq + ", contentType=" + contentType);
    }
}
