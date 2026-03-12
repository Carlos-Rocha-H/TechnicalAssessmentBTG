package com.technicalassessment.btgpactual.exception;

import com.technicalassessment.btgpactual.dto.ErrorResponse;
import com.technicalassessment.btgpactual.security.AccessForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleAccessForbidden(AccessForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .error("FORBIDDEN")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder()
                        .error("UNAUTHORIZED")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .error("INSUFFICIENT_BALANCE")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(BelowMinimumAmountException.class)
    public ResponseEntity<ErrorResponse> handleBelowMinimumAmount(BelowMinimumAmountException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .error("BELOW_MINIMUM_AMOUNT")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(DuplicateSubscriptionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSubscription(DuplicateSubscriptionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .error("DUPLICATE_SUBSCRIPTION")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClientNotFound(ClientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .error("CLIENT_NOT_FOUND")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(FundNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFundNotFound(FundNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .error("FUND_NOT_FOUND")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .error("SUBSCRIPTION_NOT_FOUND")
                        .message(ex.getMessage())
                        .build());
    }
}
