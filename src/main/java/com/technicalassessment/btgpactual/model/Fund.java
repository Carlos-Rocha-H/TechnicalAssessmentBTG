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
public class Fund {

    private String fundId;
    private String name;
    private Double minimumAmount;
    private String category;

    @DynamoDbPartitionKey
    public String getFundId() {
        return fundId;
    }
}
