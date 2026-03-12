package com.technicalassessment.btgpactual.service;

import com.technicalassessment.btgpactual.dto.SubscriptionResponse;
import com.technicalassessment.btgpactual.exception.ClientNotFoundException;
import com.technicalassessment.btgpactual.exception.DuplicateSubscriptionException;
import com.technicalassessment.btgpactual.exception.FundNotFoundException;
import com.technicalassessment.btgpactual.exception.InsufficientBalanceException;
import com.technicalassessment.btgpactual.exception.BelowMinimumAmountException;
import com.technicalassessment.btgpactual.exception.SubscriptionNotFoundException;
import com.technicalassessment.btgpactual.model.Client;
import com.technicalassessment.btgpactual.model.Fund;
import com.technicalassessment.btgpactual.model.Subscription;
import com.technicalassessment.btgpactual.repository.ClientRepository;
import com.technicalassessment.btgpactual.repository.FundRepository;
import com.technicalassessment.btgpactual.repository.SubscriptionRepository;
import com.technicalassessment.btgpactual.repository.TransactionRepository;
import com.technicalassessment.btgpactual.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private FundRepository fundRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Client client;
    private Fund fund;

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .clientId("client-001")
                .name("Cliente Demo")
                .email("demo@btgpactual.com")
                .phone("+573001234567")
                .notificationPreference("EMAIL")
                .balance(500000.0)
                .build();

        fund = Fund.builder()
                .fundId("1")
                .name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minimumAmount(75000.0)
                .category("FPV")
                .build();
    }

    @Test
    void subscribe_shouldCreateSubscriptionAndDebitBalance() {
        // CA-01: saldo >= monto mínimo → crea suscripción, debita monto, retorna 201
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        SubscriptionResponse response = subscriptionService.subscribe("client-001", "1", null);

        assertNotNull(response.getTransactionId());
        assertNotNull(response.getSubscriptionId());
        assertEquals("client-001", response.getClientId());
        assertEquals("1", response.getFundId());
        assertEquals("FPV_BTG_PACTUAL_RECAUDADORA", response.getFundName());
        assertEquals("SUBSCRIPTION", response.getType());
        assertEquals(75000.0, response.getAmount());
        assertEquals(425000.0, response.getBalance());
        assertNotNull(response.getTimestamp());

        verify(clientRepository).save(any(Client.class));
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(transactionRepository).save(any());
    }

    @Test
    void subscribe_shouldThrowWhenInsufficientBalance() {
        // CA-02: saldo insuficiente → lanza InsufficientBalanceException
        client.setBalance(50000.0);
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class,
                () -> subscriptionService.subscribe("client-001", "1", null));

        assertEquals("No tiene saldo disponible para vincularse al fondo FPV_BTG_PACTUAL_RECAUDADORA", ex.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void subscribe_shouldGenerateUniqueTransactionId() {
        // CA-03: transacción con identificador único
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        SubscriptionResponse response = subscriptionService.subscribe("client-001", "1", null);

        assertNotNull(response.getTransactionId());
        assertFalse(response.getTransactionId().isEmpty());
    }

    @Test
    void subscribe_shouldThrowWhenClientNotFound() {
        when(clientRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class,
                () -> subscriptionService.subscribe("invalid", "1", null));
    }

    @Test
    void subscribe_shouldThrowWhenFundNotFound() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("999")).thenReturn(Optional.empty());

        assertThrows(FundNotFoundException.class,
                () -> subscriptionService.subscribe("client-001", "999", null));
    }

    @Test
    void subscribe_shouldThrowWhenAlreadySubscribed() {
        Subscription existing = Subscription.builder()
                .clientId("client-001").fundId("1").subscriptionId("SUB-OLD").build();
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateSubscriptionException.class,
                () -> subscriptionService.subscribe("client-001", "1", null));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void subscribe_shouldDebitExactMinimumAmount() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        subscriptionService.subscribe("client-001", "1", null);

        assertEquals(425000.0, client.getBalance()); // 500000 - 75000
    }

    @Test
    void subscribe_shouldWorkWithExactBalance() {
        // Saldo exactamente igual al monto mínimo
        client.setBalance(75000.0);
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        SubscriptionResponse response = subscriptionService.subscribe("client-001", "1", null);

        assertEquals(0.0, response.getBalance());
    }

    // ========== HU-02: Cancelar Suscripción ==========

    @Test
    void cancel_shouldCreditBalanceAndDeleteSubscription() {
        // CA-01: Al cancelar, el valor de vinculación se retorna al saldo
        client.setBalance(425000.0);
        Subscription subscription = Subscription.builder()
                .subscriptionId("SUB-001").clientId("client-001").fundId("1")
                .amount(75000.0).subscribedAt("2026-03-11T10:00:00Z").build();

        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.cancel("client-001", "1");

        assertEquals(500000.0, response.getBalance());
        assertEquals("CANCELLATION", response.getType());
        assertEquals(75000.0, response.getAmount());
        assertEquals("FPV_BTG_PACTUAL_RECAUDADORA", response.getFundName());
        assertEquals("SUB-001", response.getSubscriptionId());
        verify(clientRepository).save(any(Client.class));
        verify(subscriptionRepository).delete("client-001", "1");
        verify(transactionRepository).save(any());
    }

    @Test
    void cancel_shouldGenerateUniqueTransactionId() {
        // CA-02: La transacción de cancelación tiene identificador único
        Subscription subscription = Subscription.builder()
                .subscriptionId("SUB-001").clientId("client-001").fundId("1")
                .amount(75000.0).subscribedAt("2026-03-11T10:00:00Z").build();

        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.cancel("client-001", "1");

        assertNotNull(response.getTransactionId());
        assertFalse(response.getTransactionId().isEmpty());
    }

    @Test
    void cancel_shouldRemoveSubscription() {
        // CA-03: El fondo cancelado ya no aparece como suscripción activa
        Subscription subscription = Subscription.builder()
                .subscriptionId("SUB-001").clientId("client-001").fundId("1")
                .amount(75000.0).subscribedAt("2026-03-11T10:00:00Z").build();

        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.of(subscription));

        subscriptionService.cancel("client-001", "1");

        verify(subscriptionRepository).delete("client-001", "1");
    }

    @Test
    void cancel_shouldThrowWhenSubscriptionNotFound() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class,
                () -> subscriptionService.cancel("client-001", "1"));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void cancel_shouldThrowWhenClientNotFound() {
        when(clientRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class,
                () -> subscriptionService.cancel("invalid", "1"));
    }

    @Test
    void cancel_shouldThrowWhenFundNotFound() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("999")).thenReturn(Optional.empty());

        assertThrows(FundNotFoundException.class,
                () -> subscriptionService.cancel("client-001", "999"));
    }

    // ========== HU-04: Notificaciones ==========

    @Test
    void subscribe_shouldSendNotificationAfterSuccess() {
        // CA-01: Al suscribirse exitosamente, se envía notificación
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        subscriptionService.subscribe("client-001", "1", null);

        verify(notificationService).notifySubscription(client, fund);
    }

    @Test
    void subscribe_shouldNotSendNotificationWhenFails() {
        // No debe notificar si la suscripción falla
        client.setBalance(0.0);
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        assertThrows(InsufficientBalanceException.class,
                () -> subscriptionService.subscribe("client-001", "1", null));

        verify(notificationService, never()).notifySubscription(any(), any());
    }

    // ========== HU-01 CA-03 / HU-02 CA-02: Unicidad de transactionId ==========

    @Test
    void subscribe_shouldGenerateDistinctTransactionIdsForDifferentSubscriptions() {
        Fund fund2 = Fund.builder().fundId("2").name("FPV_BTG_PACTUAL_ECOPETROL")
                .minimumAmount(125000.0).category("FPV").build();

        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(fundRepository.findById("2")).thenReturn(Optional.of(fund2));
        when(subscriptionRepository.findByClientIdAndFundId(anyString(), anyString())).thenReturn(Optional.empty());

        SubscriptionResponse r1 = subscriptionService.subscribe("client-001", "1", null);
        SubscriptionResponse r2 = subscriptionService.subscribe("client-001", "2", null);

        assertNotEquals(r1.getTransactionId(), r2.getTransactionId());
        assertNotEquals(r1.getSubscriptionId(), r2.getSubscriptionId());
    }

    @Test
    void cancel_shouldGenerateTransactionIdDistinctFromSubscription() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1"))
                .thenReturn(Optional.empty())  // para subscribe
                .thenReturn(Optional.of(Subscription.builder()
                        .subscriptionId("SUB-001").clientId("client-001").fundId("1")
                        .amount(75000.0).subscribedAt("2026-03-11T10:00:00Z").build()));  // para cancel

        SubscriptionResponse subResponse = subscriptionService.subscribe("client-001", "1", null);
        SubscriptionResponse cancelResponse = subscriptionService.cancel("client-001", "1");

        assertNotEquals(subResponse.getTransactionId(), cancelResponse.getTransactionId());
    }

    // ========== Amount personalizado ==========

    @Test
    void subscribe_shouldUseCustomAmountWhenProvided() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        SubscriptionResponse response = subscriptionService.subscribe("client-001", "1", 100000.0);

        assertEquals(100000.0, response.getAmount());
        assertEquals(400000.0, response.getBalance()); // 500000 - 100000
    }

    @Test
    void subscribe_shouldThrowWhenAmountBelowMinimum() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));

        BelowMinimumAmountException ex = assertThrows(BelowMinimumAmountException.class,
                () -> subscriptionService.subscribe("client-001", "1", 50000.0));

        assertTrue(ex.getMessage().contains("inferior al mínimo requerido"));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void subscribe_shouldAcceptExactMinimumAmount() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientIdAndFundId("client-001", "1")).thenReturn(Optional.empty());

        SubscriptionResponse response = subscriptionService.subscribe("client-001", "1", 75000.0);

        assertEquals(75000.0, response.getAmount());
        assertEquals(425000.0, response.getBalance());
    }
}
