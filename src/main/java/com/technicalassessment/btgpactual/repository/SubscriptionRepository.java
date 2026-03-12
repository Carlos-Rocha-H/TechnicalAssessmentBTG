package com.technicalassessment.btgpactual.repository;

import com.technicalassessment.btgpactual.model.Subscription;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;

@Repository
public class SubscriptionRepository {

    private final DynamoDbTable<Subscription> table;

    public SubscriptionRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Subscriptions", TableSchema.fromBean(Subscription.class));
    }

    public void save(Subscription subscription) {
        table.putItem(subscription);
    }

    public Optional<Subscription> findByClientIdAndFundId(String clientId, String fundId) {
        Subscription sub = table.getItem(Key.builder()
                .partitionValue(clientId)
                .sortValue(fundId)
                .build());
        return Optional.ofNullable(sub);
    }

    public List<Subscription> findByClientId(String clientId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(clientId).build());

        return table.query(queryConditional)
                .items()
                .stream()
                .toList();
    }

    public void delete(String clientId, String fundId) {
        table.deleteItem(Key.builder()
                .partitionValue(clientId)
                .sortValue(fundId)
                .build());
    }

    public DynamoDbTable<Subscription> getTable() {
        return table;
    }
}
