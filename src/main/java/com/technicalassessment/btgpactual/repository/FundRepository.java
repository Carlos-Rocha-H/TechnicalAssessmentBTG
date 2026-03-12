package com.technicalassessment.btgpactual.repository;

import com.technicalassessment.btgpactual.model.Fund;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;

@Repository
public class FundRepository {

    private final DynamoDbTable<Fund> table;

    public FundRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Funds", TableSchema.fromBean(Fund.class));
    }

    public void save(Fund fund) {
        table.putItem(fund);
    }

    public Optional<Fund> findById(String fundId) {
        Fund fund = table.getItem(Key.builder().partitionValue(fundId).build());
        return Optional.ofNullable(fund);
    }

    public List<Fund> findAll() {
        return table.scan().items().stream().toList();
    }

    public DynamoDbTable<Fund> getTable() {
        return table;
    }
}
