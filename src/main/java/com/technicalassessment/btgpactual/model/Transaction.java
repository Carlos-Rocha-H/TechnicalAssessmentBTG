package com.technicalassessment.btgpactual.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Transaction {

    private String transactionId;
    private String subscriptionId;
    private String clientId;
    private String fundId;
    private String fundName;
    private String type;
    private Double amount;
    private String timestamp;

    @DynamoDbPartitionKey
    public String getTransactionId() {
        return transactionId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "clientId-index")
    public String getClientId() {
        return clientId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "subscriptionId-index")
    public String getSubscriptionId() {
        return subscriptionId;
    }
}
