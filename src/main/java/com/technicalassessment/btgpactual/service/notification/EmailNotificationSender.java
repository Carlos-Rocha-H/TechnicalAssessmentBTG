package com.technicalassessment.btgpactual.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Component
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final SesClient sesClient;
    private final String fromEmail;

    public EmailNotificationSender(SesClient sesClient,
                                   @Value("${aws.ses.from:noreply@btgpactual.com}") String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String destination, String subject, String message) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(destination)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder()
                                    .text(Content.builder().data(message).build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("EMAIL enviado a {} via SES (MessageId: {}): [{}]", destination, response.messageId(), subject);
        } catch (SesException e) {
            log.error("Error enviando EMAIL via SES a {}: {}", destination, e.getMessage());
        }
    }

    @Override
    public String getType() {
        return "EMAIL";
    }
}
