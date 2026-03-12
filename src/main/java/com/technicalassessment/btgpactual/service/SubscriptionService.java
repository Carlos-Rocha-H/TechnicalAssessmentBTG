package com.technicalassessment.btgpactual.service;

import com.technicalassessment.btgpactual.dto.SubscriptionResponse;
import com.technicalassessment.btgpactual.exception.BelowMinimumAmountException;
import com.technicalassessment.btgpactual.exception.ClientNotFoundException;
import com.technicalassessment.btgpactual.exception.DuplicateSubscriptionException;
import com.technicalassessment.btgpactual.exception.FundNotFoundException;
import com.technicalassessment.btgpactual.exception.InsufficientBalanceException;
import com.technicalassessment.btgpactual.exception.SubscriptionNotFoundException;
import com.technicalassessment.btgpactual.model.Client;
import com.technicalassessment.btgpactual.model.Fund;
import com.technicalassessment.btgpactual.model.Subscription;
import com.technicalassessment.btgpactual.model.Transaction;
import com.technicalassessment.btgpactual.repository.ClientRepository;
import com.technicalassessment.btgpactual.repository.FundRepository;
import com.technicalassessment.btgpactual.repository.SubscriptionRepository;
import com.technicalassessment.btgpactual.repository.TransactionRepository;
import com.technicalassessment.btgpactual.service.notification.NotificationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final ClientRepository clientRepository;
    private final FundRepository fundRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public SubscriptionService(ClientRepository clientRepository,
                                FundRepository fundRepository,
                                SubscriptionRepository subscriptionRepository,
                                TransactionRepository transactionRepository,
                                NotificationService notificationService) {
        this.clientRepository = clientRepository;
        this.fundRepository = fundRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    public SubscriptionResponse subscribe(String clientId, String fundId, Double amount) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        // Determinar monto: si no se envía, usar el mínimo del fondo
        double subscriptionAmount = (amount != null) ? amount : fund.getMinimumAmount();

        // Validar que el monto sea >= al mínimo del fondo
        if (subscriptionAmount < fund.getMinimumAmount()) {
            throw new BelowMinimumAmountException(subscriptionAmount, fund.getMinimumAmount(), fund.getName());
        }

        // Verificar que no esté ya suscrito
        subscriptionRepository.findByClientIdAndFundId(clientId, fundId)
                .ifPresent(s -> { throw new DuplicateSubscriptionException(fund.getName()); });

        // Verificar saldo suficiente
        if (client.getBalance() < subscriptionAmount) {
            throw new InsufficientBalanceException(fund.getName());
        }

        // Debitar saldo
        double newBalance = client.getBalance() - subscriptionAmount;
        client.setBalance(newBalance);
        clientRepository.save(client);

        // Crear suscripción
        String subscriptionId = UUID.randomUUID().toString();
        String now = Instant.now().toString();

        Subscription subscription = Subscription.builder()
                .subscriptionId(subscriptionId)
                .clientId(clientId)
                .fundId(fundId)
                .subscribedAt(now)
                .amount(subscriptionAmount)
                .build();
        subscriptionRepository.save(subscription);

        // Registrar transacción
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .subscriptionId(subscriptionId)
                .clientId(clientId)
                .fundId(fundId)
                .fundName(fund.getName())
                .type("SUBSCRIPTION")
                .amount(subscriptionAmount)
                .timestamp(now)
                .build();
        transactionRepository.save(transaction);

        // Notificar al cliente (HU-04)
        notificationService.notifySubscription(client, fund);

        return SubscriptionResponse.builder()
                .transactionId(transactionId)
                .subscriptionId(subscriptionId)
                .clientId(clientId)
                .fundId(fundId)
                .fundName(fund.getName())
                .type("SUBSCRIPTION")
                .amount(subscriptionAmount)
                .balance(newBalance)
                .timestamp(now)
                .build();
    }

    public SubscriptionResponse cancel(String clientId, String fundId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        Subscription subscription = subscriptionRepository.findByClientIdAndFundId(clientId, fundId)
                .orElseThrow(() -> new SubscriptionNotFoundException(clientId, fundId));

        // Acreditar saldo
        double newBalance = client.getBalance() + subscription.getAmount();
        client.setBalance(newBalance);
        clientRepository.save(client);

        // Eliminar suscripción
        subscriptionRepository.delete(clientId, fundId);

        // Registrar transacción de cancelación
        String transactionId = UUID.randomUUID().toString();
        String now = Instant.now().toString();

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .subscriptionId(subscription.getSubscriptionId())
                .clientId(clientId)
                .fundId(fundId)
                .fundName(fund.getName())
                .type("CANCELLATION")
                .amount(subscription.getAmount())
                .timestamp(now)
                .build();
        transactionRepository.save(transaction);

        return SubscriptionResponse.builder()
                .transactionId(transactionId)
                .subscriptionId(subscription.getSubscriptionId())
                .clientId(clientId)
                .fundId(fundId)
                .fundName(fund.getName())
                .type("CANCELLATION")
                .amount(subscription.getAmount())
                .balance(newBalance)
                .timestamp(now)
                .build();
    }
}
