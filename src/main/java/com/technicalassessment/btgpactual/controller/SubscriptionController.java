package com.technicalassessment.btgpactual.controller;

import com.technicalassessment.btgpactual.dto.SubscriptionRequest;
import com.technicalassessment.btgpactual.dto.SubscriptionResponse;
import com.technicalassessment.btgpactual.security.OwnershipValidator;
import com.technicalassessment.btgpactual.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final OwnershipValidator ownershipValidator;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  OwnershipValidator ownershipValidator) {
        this.subscriptionService = subscriptionService;
        this.ownershipValidator = ownershipValidator;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(
            @PathVariable String clientId,
            @RequestBody SubscriptionRequest request) {
        ownershipValidator.validateOwnership(clientId);
        SubscriptionResponse response = subscriptionService.subscribe(clientId, request.getFundId(), request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{fundId}")
    public ResponseEntity<SubscriptionResponse> cancel(
            @PathVariable String clientId,
            @PathVariable String fundId) {
        ownershipValidator.validateOwnership(clientId);
        SubscriptionResponse response = subscriptionService.cancel(clientId, fundId);
        return ResponseEntity.ok(response);
    }
}
