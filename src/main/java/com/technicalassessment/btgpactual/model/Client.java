package com.technicalassessment.btgpactual.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Client {

    private String clientId;
    private String name;
    private String email;
    private String phone;
    private String notificationPreference;
    private Double balance;

    @DynamoDbPartitionKey
    public String getClientId() {
        return clientId;
    }
}
