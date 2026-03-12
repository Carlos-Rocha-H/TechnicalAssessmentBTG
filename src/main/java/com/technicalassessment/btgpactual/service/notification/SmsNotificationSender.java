package com.technicalassessment.btgpactual.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

@Component
public class SmsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    private final SnsClient snsClient;

    public SmsNotificationSender(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @Override
    public void send(String destination, String subject, String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .phoneNumber(destination)
                    .message(message)
                    .build();

            PublishResponse response = snsClient.publish(request);
            log.info("SMS enviado a {} via SNS (MessageId: {})", destination, response.messageId());
        } catch (SnsException e) {
            log.error("Error enviando SMS via SNS a {}: {}", destination, e.getMessage());
        }
    }

    @Override
    public String getType() {
        return "SMS";
    }
}
