package com.ideatrack.main.exception;

public class OnlyOneMandatoryObjectiveAllowedException extends RuntimeException {
    public OnlyOneMandatoryObjectiveAllowedException() {
        super("Only one objective can be mandatory. Unset others first.");
    }
}