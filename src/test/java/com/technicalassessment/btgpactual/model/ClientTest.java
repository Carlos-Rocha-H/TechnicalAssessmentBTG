package com.technicalassessment.btgpactual.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    @Test
    void shouldCreateClientWithBuilder() {
        Client client = Client.builder()
                .clientId("client-001")
                .name("Juan Pérez")
                .email("juan@email.com")
                .phone("+573001234567")
                .notificationPreference("EMAIL")
                .balance(500000.0)
                .build();

        assertEquals("client-001", client.getClientId());
        assertEquals("Juan Pérez", client.getName());
        assertEquals("juan@email.com", client.getEmail());
        assertEquals("+573001234567", client.getPhone());
        assertEquals("EMAIL", client.getNotificationPreference());
        assertEquals(500000.0, client.getBalance());
    }

    @Test
    void shouldCreateClientWithNoArgsConstructor() {
        Client client = new Client();
        client.setClientId("c-1");
        client.setBalance(100000.0);

        assertEquals("c-1", client.getClientId());
        assertEquals(100000.0, client.getBalance());
    }
}
