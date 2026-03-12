package com.technicalassessment.btgpactual.exception;

public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(String clientId, String fundId) {
        super("El cliente " + clientId + " no tiene suscripción activa al fondo " + fundId);
    }
}
