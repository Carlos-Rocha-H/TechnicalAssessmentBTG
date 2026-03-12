package com.technicalassessment.btgpactual.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private String transactionId;
    private String subscriptionId;
    private String clientId;
    private String fundId;
    private String fundName;
    private String type;
    private Double amount;
    private Double balance;
    private String timestamp;
}
