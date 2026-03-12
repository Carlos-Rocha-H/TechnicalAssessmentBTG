package com.technicalassessment.btgpactual.exception;

public class BelowMinimumAmountException extends RuntimeException {
    public BelowMinimumAmountException(double amount, double minimumAmount, String fundName) {
        super(String.format("El monto $%,.0f es inferior al mínimo requerido $%,.0f para el fondo %s",
                amount, minimumAmount, fundName));
    }
}
