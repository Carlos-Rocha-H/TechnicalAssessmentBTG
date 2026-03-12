package com.technicalassessment.btgpactual.controller;

import com.technicalassessment.btgpactual.dto.SubscriptionResponse;
import com.technicalassessment.btgpactual.exception.GlobalExceptionHandler;
import com.technicalassessment.btgpactual.exception.InsufficientBalanceException;
import com.technicalassessment.btgpactual.exception.SubscriptionNotFoundException;
import com.technicalassessment.btgpactual.security.AccessForbiddenException;
import com.technicalassessment.btgpactual.security.OwnershipValidator;
import com.technicalassessment.btgpactual.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private OwnershipValidator ownershipValidator;

    @InjectMocks
    private SubscriptionController subscriptionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subscriptionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void subscribe_shouldReturn201WhenSuccessful() throws Exception {
        SubscriptionResponse response = SubscriptionResponse.builder()
                .transactionId("TXN-001")
                .subscriptionId("SUB-001")
                .clientId("client-001")
                .fundId("1")
                .fundName("FPV_BTG_PACTUAL_RECAUDADORA")
                .type("SUBSCRIPTION")
                .amount(75000.0)
                .balance(425000.0)
                .timestamp("2026-03-11T10:30:00Z")
                .build();

        when(subscriptionService.subscribe(eq("client-001"), eq("1"), isNull())).thenReturn(response);

        mockMvc.perform(post("/api/v1/clients/client-001/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fundId\":\"1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-001"))
                .andExpect(jsonPath("$.clientId").value("client-001"))
                .andExpect(jsonPath("$.fundId").value("1"))
                .andExpect(jsonPath("$.fundName").value("FPV_BTG_PACTUAL_RECAUDADORA"))
                .andExpect(jsonPath("$.type").value("SUBSCRIPTION"))
                .andExpect(jsonPath("$.amount").value(75000.0))
                .andExpect(jsonPath("$.balance").value(425000.0));
    }

    @Test
    void subscribe_shouldReturn400WhenInsufficientBalance() throws Exception {
        when(subscriptionService.subscribe(eq("client-001"), eq("1"), isNull()))
                .thenThrow(new InsufficientBalanceException("FPV_BTG_PACTUAL_RECAUDADORA"));

        mockMvc.perform(post("/api/v1/clients/client-001/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fundId\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").value("No tiene saldo disponible para vincularse al fondo FPV_BTG_PACTUAL_RECAUDADORA"));
    }

    @Test
    void cancel_shouldReturn200WhenSuccessful() throws Exception {
        SubscriptionResponse response = SubscriptionResponse.builder()
                .transactionId("TXN-002")
                .subscriptionId("SUB-001")
                .clientId("client-001")
                .fundId("1")
                .fundName("FPV_BTG_PACTUAL_RECAUDADORA")
                .type("CANCELLATION")
                .amount(75000.0)
                .balance(500000.0)
                .timestamp("2026-03-11T11:00:00Z")
                .build();

        when(subscriptionService.cancel(eq("client-001"), eq("1"))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/clients/client-001/subscriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-002"))
                .andExpect(jsonPath("$.type").value("CANCELLATION"))
                .andExpect(jsonPath("$.amount").value(75000.0))
                .andExpect(jsonPath("$.balance").value(500000.0));
    }

    @Test
    void cancel_shouldReturn404WhenSubscriptionNotFound() throws Exception {
        when(subscriptionService.cancel(eq("client-001"), eq("1")))
                .thenThrow(new SubscriptionNotFoundException("client-001", "1"));

        mockMvc.perform(delete("/api/v1/clients/client-001/subscriptions/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SUBSCRIPTION_NOT_FOUND"));
    }

    // ========== HU-06 CA-02: Ownership Validation ==========

    @Test
    void subscribe_shouldCallOwnershipValidation() throws Exception {
        SubscriptionResponse response = SubscriptionResponse.builder()
                .transactionId("TXN-001").subscriptionId("SUB-001").clientId("client-001")
                .fundId("1").fundName("FPV_BTG_PACTUAL_RECAUDADORA").type("SUBSCRIPTION")
                .amount(75000.0).balance(425000.0).timestamp("2026-03-11T10:30:00Z").build();
        when(subscriptionService.subscribe(eq("client-001"), eq("1"), isNull())).thenReturn(response);

        mockMvc.perform(post("/api/v1/clients/client-001/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fundId\":\"1\"}"))
                .andExpect(status().isCreated());

        verify(ownershipValidator).validateOwnership("client-001");
    }

    @Test
    void cancel_shouldCallOwnershipValidation() throws Exception {
        SubscriptionResponse response = SubscriptionResponse.builder()
                .transactionId("TXN-002").subscriptionId("SUB-001").clientId("client-001")
                .fundId("1").fundName("FPV_BTG_PACTUAL_RECAUDADORA").type("CANCELLATION")
                .amount(75000.0).balance(500000.0).timestamp("2026-03-11T11:00:00Z").build();
        when(subscriptionService.cancel(eq("client-001"), eq("1"))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/clients/client-001/subscriptions/1"))
                .andExpect(status().isOk());

        verify(ownershipValidator).validateOwnership("client-001");
    }

    @Test
    void subscribe_shouldReturn403WhenNotResourceOwner() throws Exception {
        doThrow(new AccessForbiddenException("No tiene permisos para acceder a los recursos del cliente client-001"))
                .when(ownershipValidator).validateOwnership("client-001");

        mockMvc.perform(post("/api/v1/clients/client-001/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fundId\":\"1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void cancel_shouldReturn403WhenNotResourceOwner() throws Exception {
        doThrow(new AccessForbiddenException("No tiene permisos para acceder a los recursos del cliente client-001"))
                .when(ownershipValidator).validateOwnership("client-001");

        mockMvc.perform(delete("/api/v1/clients/client-001/subscriptions/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
