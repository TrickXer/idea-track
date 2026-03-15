package com.ideatrack.main.exception;

public class DepartmentNotFound extends RuntimeException {
    public DepartmentNotFound(String msg) {
        super(msg);
    }
}
