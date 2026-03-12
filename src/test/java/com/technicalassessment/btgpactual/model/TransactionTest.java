package com.technicalassessment.btgpactual.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void shouldCreateSubscriptionTransaction() {
        Transaction tx = Transaction.builder()
                .transactionId("TXN-001")
                .subscriptionId("SUB-001")
                .clientId("client-001")
                .fundId("1")
                .fundName("FPV_BTG_PACTUAL_RECAUDADORA")
                .type("SUBSCRIPTION")
                .amount(75000.0)
                .timestamp("2026-03-11T10:30:00Z")
                .build();

        assertEquals("TXN-001", tx.getTransactionId());
        assertEquals("SUB-001", tx.getSubscriptionId());
        assertEquals("client-001", tx.getClientId());
        assertEquals("SUBSCRIPTION", tx.getType());
        assertEquals(75000.0, tx.getAmount());
    }

    @Test
    void shouldCreateCancellationTransaction() {
        Transaction tx = Transaction.builder()
                .transactionId("TXN-002")
                .subscriptionId("SUB-001")
                .clientId("client-001")
                .fundId("1")
                .fundName("FPV_BTG_PACTUAL_RECAUDADORA")
                .type("CANCELLATION")
                .amount(75000.0)
                .timestamp("2026-03-11T11:00:00Z")
                .build();

        assertEquals("CANCELLATION", tx.getType());
        assertEquals("SUB-001", tx.getSubscriptionId());
        assertEquals("1", tx.getFundId());
    }

    @Test
    void shouldRelateMultipleTransactionsToSameSubscription() {
        String subscriptionId = "SUB-001";

        Transaction open = Transaction.builder()
                .transactionId("TXN-001").subscriptionId(subscriptionId)
                .type("SUBSCRIPTION").amount(75000.0).build();
        Transaction close = Transaction.builder()
                .transactionId("TXN-002").subscriptionId(subscriptionId)
                .type("CANCELLATION").amount(75000.0).build();

        assertEquals(open.getSubscriptionId(), close.getSubscriptionId());
        assertNotEquals(open.getTransactionId(), close.getTransactionId());
    }
}
