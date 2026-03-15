package com.ideatrack.main.exception;

public class UserIdRequiredException extends RuntimeException{

    public UserIdRequiredException() {
        super("userId is required");
    }
}