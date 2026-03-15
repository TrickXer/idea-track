package com.ideatrack.main.exception;

public class ProposalAlreadyExistsException extends RuntimeException{

//e.g., com.ideatrack.main.exception.ProposalAlreadyExists
 public ProposalAlreadyExistsException(Integer ideaId) {
     super("A proposal already exists for idea: " + ideaId);
 }
}