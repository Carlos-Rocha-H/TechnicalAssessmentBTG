package com.technicalassessment.btgpactual.service.notification;

import com.technicalassessment.btgpactual.model.Client;
import com.technicalassessment.btgpactual.model.Fund;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Test
    void shouldSendEmailWhenPreferenceIsEmail() {
        // CA-01 + CA-02: notificación por email según preferencia
        NotificationSender emailSender = mock(NotificationSender.class);
        when(emailSender.getType()).thenReturn("EMAIL");

        NotificationService service = new NotificationService(List.of(emailSender));

        Client client = Client.builder()
                .clientId("client-001").name("Cliente Demo")
                .email("demo@btgpactual.com").phone("+573001234567")
                .notificationPreference("EMAIL").balance(500000.0).build();

        Fund fund = Fund.builder()
                .fundId("1").name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minimumAmount(75000.0).category("FPV").build();

        service.notifySubscription(client, fund);

        // CA-03: notificación incluye info del fondo
        verify(emailSender).send(
                eq("demo@btgpactual.com"),
                contains("FPV_BTG_PACTUAL_RECAUDADORA"),
                contains("FPV_BTG_PACTUAL_RECAUDADORA"));
    }

    @Test
    void shouldSendSmsWhenPreferenceIsSms() {
        // CA-01 + CA-02: notificación por SMS según preferencia
        NotificationSender smsSender = mock(NotificationSender.class);
        when(smsSender.getType()).thenReturn("SMS");

        NotificationService service = new NotificationService(List.of(smsSender));

        Client client = Client.builder()
                .clientId("client-001").name("Cliente Demo")
                .email("demo@btgpactual.com").phone("+573001234567")
                .notificationPreference("SMS").balance(500000.0).build();

        Fund fund = Fund.builder()
                .fundId("1").name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minimumAmount(75000.0).category("FPV").build();

        service.notifySubscription(client, fund);

        verify(smsSender).send(
                eq("+573001234567"),
                contains("FPV_BTG_PACTUAL_RECAUDADORA"),
                contains("FPV_BTG_PACTUAL_RECAUDADORA"));
    }

    @Test
    void shouldNotFailWithUnsupportedPreference() {
        // Preferencia no soportada no debe lanzar excepción
        NotificationSender emailSender = mock(NotificationSender.class);
        when(emailSender.getType()).thenReturn("EMAIL");

        NotificationService service = new NotificationService(List.of(emailSender));

        Client client = Client.builder()
                .clientId("client-001").name("Cliente Demo")
                .notificationPreference("PUSH").balance(500000.0).build();

        Fund fund = Fund.builder()
                .fundId("1").name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minimumAmount(75000.0).category("FPV").build();

        // No debe lanzar excepción
        service.notifySubscription(client, fund);

        verify(emailSender, never()).send(any(), any(), any());
    }
}
