package com.technicalassessment.btgpactual.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(String clientId) {
        super("Cliente no encontrado: " + clientId);
    }
}
