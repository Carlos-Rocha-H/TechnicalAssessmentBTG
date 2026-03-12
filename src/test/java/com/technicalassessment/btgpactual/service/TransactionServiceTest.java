package com.technicalassessment.btgpactual.service;

import com.technicalassessment.btgpactual.dto.TransactionResponse;
import com.technicalassessment.btgpactual.exception.ClientNotFoundException;
import com.technicalassessment.btgpactual.model.Client;
import com.technicalassessment.btgpactual.model.Transaction;
import com.technicalassessment.btgpactual.repository.ClientRepository;
import com.technicalassessment.btgpactual.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void getTransactions_shouldReturnAllClientTransactions() {
        // CA-01: retorna lista incluyendo aperturas y cancelaciones
        Client client = Client.builder().clientId("client-001").name("Demo").balance(500000.0).build();
        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .transactionId("TXN-001").clientId("client-001").fundId("1")
                        .fundName("FPV_BTG_PACTUAL_RECAUDADORA").type("SUBSCRIPTION")
                        .amount(75000.0).timestamp("2026-03-11T10:30:00Z").build(),
                Transaction.builder()
                        .transactionId("TXN-002").clientId("client-001").fundId("1")
                        .fundName("FPV_BTG_PACTUAL_RECAUDADORA").type("CANCELLATION")
                        .amount(75000.0).timestamp("2026-03-11T11:00:00Z").build()
        );

        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(transactionRepository.findByClientId("client-001")).thenReturn(transactions);

        List<TransactionResponse> result = transactionService.getTransactionsByClientId("client-001");

        assertEquals(2, result.size());
        assertEquals("SUBSCRIPTION", result.get(0).getType());
        assertEquals("CANCELLATION", result.get(1).getType());
    }

    @Test
    void getTransactions_shouldReturnCorrectFields() {
        // CA-02: cada transacción incluye id, fondo, tipo, monto y fecha
        Client client = Client.builder().clientId("client-001").name("Demo").balance(500000.0).build();
        Transaction tx = Transaction.builder()
                .transactionId("TXN-001").clientId("client-001").fundId("1")
                .fundName("FPV_BTG_PACTUAL_RECAUDADORA").type("SUBSCRIPTION")
                .amount(75000.0).timestamp("2026-03-11T10:30:00Z").build();

        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(transactionRepository.findByClientId("client-001")).thenReturn(List.of(tx));

        List<TransactionResponse> result = transactionService.getTransactionsByClientId("client-001");

        TransactionResponse r = result.get(0);
        assertEquals("TXN-001", r.getTransactionId());
        assertEquals("1", r.getFundId());
        assertEquals("FPV_BTG_PACTUAL_RECAUDADORA", r.getFundName());
        assertEquals("SUBSCRIPTION", r.getType());
        assertEquals(75000.0, r.getAmount());
        assertEquals("2026-03-11T10:30:00Z", r.getTimestamp());
    }

    @Test
    void getTransactions_shouldReturnEmptyListWhenNoTransactions() {
        Client client = Client.builder().clientId("client-001").name("Demo").balance(500000.0).build();

        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(transactionRepository.findByClientId("client-001")).thenReturn(List.of());

        List<TransactionResponse> result = transactionService.getTransactionsByClientId("client-001");

        assertTrue(result.isEmpty());
    }

    @Test
    void getTransactions_shouldThrowWhenClientNotFound() {
        when(clientRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class,
                () -> transactionService.getTransactionsByClientId("invalid"));
    }
}
