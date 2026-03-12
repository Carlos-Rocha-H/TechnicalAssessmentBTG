package com.technicalassessment.btgpactual.controller;

import com.technicalassessment.btgpactual.dto.TransactionResponse;
import com.technicalassessment.btgpactual.security.OwnershipValidator;
import com.technicalassessment.btgpactual.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final OwnershipValidator ownershipValidator;

    public TransactionController(TransactionService transactionService,
                                 OwnershipValidator ownershipValidator) {
        this.transactionService = transactionService;
        this.ownershipValidator = ownershipValidator;
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable String clientId) {
        ownershipValidator.validateOwnership(clientId);
        List<TransactionResponse> transactions = transactionService.getTransactionsByClientId(clientId);
        return ResponseEntity.ok(transactions);
    }
}
