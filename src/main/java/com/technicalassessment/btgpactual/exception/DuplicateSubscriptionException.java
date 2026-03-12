package com.technicalassessment.btgpactual.exception;

public class DuplicateSubscriptionException extends RuntimeException {
    public DuplicateSubscriptionException(String fundName) {
        super("Ya se encuentra vinculado al fondo " + fundName);
    }
}
