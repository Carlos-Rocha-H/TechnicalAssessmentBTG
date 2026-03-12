package com.technicalassessment.btgpactual.service.notification;

public interface NotificationSender {
    void send(String destination, String subject, String message);
    String getType();
}
