package com.technicalassessment.btgpactual.service;

import com.technicalassessment.btgpactual.dto.TransactionResponse;
import com.technicalassessment.btgpactual.exception.ClientNotFoundException;
import com.technicalassessment.btgpactual.repository.ClientRepository;
import com.technicalassessment.btgpactual.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ClientRepository clientRepository;

    public TransactionService(TransactionRepository transactionRepository,
                               ClientRepository clientRepository) {
        this.transactionRepository = transactionRepository;
        this.clientRepository = clientRepository;
    }

    public List<TransactionResponse> getTransactionsByClientId(String clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        return transactionRepository.findByClientId(clientId).stream()
                .map(tx -> TransactionResponse.builder()
                        .transactionId(tx.getTransactionId())
                        .fundId(tx.getFundId())
                        .fundName(tx.getFundName())
                        .type(tx.getType())
                        .amount(tx.getAmount())
                        .timestamp(tx.getTimestamp())
                        .build())
                .toList();
    }
}
