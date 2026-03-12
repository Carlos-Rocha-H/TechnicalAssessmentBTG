package com.technicalassessment.btgpactual.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Subscription {

    private String subscriptionId;
    private String clientId;
    private String fundId;
    private String subscribedAt;
    private Double amount;

    @DynamoDbPartitionKey
    public String getClientId() {
        return clientId;
    }

    @DynamoDbSortKey
    public String getFundId() {
        return fundId;
    }
}
