package com.technicalassessment.btgpactual.controller;

import com.technicalassessment.btgpactual.dto.TransactionResponse;
import com.technicalassessment.btgpactual.exception.ClientNotFoundException;
import com.technicalassessment.btgpactual.exception.GlobalExceptionHandler;
import com.technicalassessment.btgpactual.security.AccessForbiddenException;
import com.technicalassessment.btgpactual.security.OwnershipValidator;
import com.technicalassessment.btgpactual.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @Mock
    private OwnershipValidator ownershipValidator;

    @InjectMocks
    private TransactionController transactionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTransactions_shouldReturn200WithList() throws Exception {
        List<TransactionResponse> transactions = List.of(
                TransactionResponse.builder()
                        .transactionId("TXN-001").fundId("1")
                        .fundName("FPV_BTG_PACTUAL_RECAUDADORA").type("SUBSCRIPTION")
                        .amount(75000.0).timestamp("2026-03-11T10:30:00Z").build(),
                TransactionResponse.builder()
                        .transactionId("TXN-002").fundId("1")
                        .fundName("FPV_BTG_PACTUAL_RECAUDADORA").type("CANCELLATION")
                        .amount(75000.0).timestamp("2026-03-11T11:00:00Z").build()
        );

        when(transactionService.getTransactionsByClientId(eq("client-001"))).thenReturn(transactions);

        mockMvc.perform(get("/api/v1/clients/client-001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionId").value("TXN-001"))
                .andExpect(jsonPath("$[0].type").value("SUBSCRIPTION"))
                .andExpect(jsonPath("$[1].transactionId").value("TXN-002"))
                .andExpect(jsonPath("$[1].type").value("CANCELLATION"));
    }

    @Test
    void getTransactions_shouldReturn200WithEmptyList() throws Exception {
        when(transactionService.getTransactionsByClientId(eq("client-001"))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/clients/client-001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getTransactions_shouldReturn404WhenClientNotFound() throws Exception {
        when(transactionService.getTransactionsByClientId(eq("invalid")))
                .thenThrow(new ClientNotFoundException("invalid"));

        mockMvc.perform(get("/api/v1/clients/invalid/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CLIENT_NOT_FOUND"));
    }

    // ========== HU-06 CA-02: Ownership Validation ==========

    @Test
    void getTransactions_shouldCallOwnershipValidation() throws Exception {
        when(transactionService.getTransactionsByClientId(eq("client-001"))).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/clients/client-001/transactions"))
                .andExpect(status().isOk());

        verify(ownershipValidator).validateOwnership("client-001");
    }

    @Test
    void getTransactions_shouldReturn403WhenNotResourceOwner() throws Exception {
        doThrow(new AccessForbiddenException("No tiene permisos para acceder a los recursos del cliente client-001"))
                .when(ownershipValidator).validateOwnership("client-001");

        mockMvc.perform(get("/api/v1/clients/client-001/transactions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
