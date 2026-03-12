package com.technicalassessment.btgpactual.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionTest {

    @Test
    void shouldCreateSubscriptionWithCompositeKey() {
        Subscription sub = Subscription.builder()
                .subscriptionId("SUB-001")
                .clientId("client-001")
                .fundId("1")
                .subscribedAt("2026-03-11T10:30:00Z")
                .amount(75000.0)
                .build();

        assertEquals("SUB-001", sub.getSubscriptionId());
        assertEquals("client-001", sub.getClientId());
        assertEquals("1", sub.getFundId());
        assertEquals("2026-03-11T10:30:00Z", sub.getSubscribedAt());
        assertEquals(75000.0, sub.getAmount());
    }

    @Test
    void shouldSupportMultipleSubscriptionsPerClient() {
        Subscription sub1 = Subscription.builder()
                .subscriptionId("SUB-001")
                .clientId("client-001").fundId("1").amount(75000.0).build();
        Subscription sub2 = Subscription.builder()
                .subscriptionId("SUB-002")
                .clientId("client-001").fundId("3").amount(50000.0).build();

        assertEquals(sub1.getClientId(), sub2.getClientId());
        assertNotEquals(sub1.getFundId(), sub2.getFundId());
        assertNotEquals(sub1.getSubscriptionId(), sub2.getSubscriptionId());
    }
}
