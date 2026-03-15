package com.ideatrack.main.exception;

public class BudgetNegativeException extends RuntimeException {
    public BudgetNegativeException(Long budget) {
        super("Budget must be ≥ 0. Provided: " + budget);
    }
}