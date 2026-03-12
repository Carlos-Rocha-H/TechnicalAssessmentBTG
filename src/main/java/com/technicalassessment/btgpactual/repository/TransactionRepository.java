package com.technicalassessment.btgpactual.repository;

import com.technicalassessment.btgpactual.model.Transaction;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private final DynamoDbTable<Transaction> table;
    private final DynamoDbIndex<Transaction> clientIdIndex;
    private final DynamoDbIndex<Transaction> subscriptionIdIndex;

    public TransactionRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Transactions", TableSchema.fromBean(Transaction.class));
        this.clientIdIndex = table.index("clientId-index");
        this.subscriptionIdIndex = table.index("subscriptionId-index");
    }

    public void save(Transaction transaction) {
        table.putItem(transaction);
    }

    public Optional<Transaction> findById(String transactionId) {
        Transaction tx = table.getItem(Key.builder().partitionValue(transactionId).build());
        return Optional.ofNullable(tx);
    }

    public List<Transaction> findByClientId(String clientId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(clientId).build());

        return clientIdIndex.query(queryConditional)
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    public List<Transaction> findBySubscriptionId(String subscriptionId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(subscriptionId).build());

        return subscriptionIdIndex.query(queryConditional)
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    public DynamoDbTable<Transaction> getTable() {
        return table;
    }
}
