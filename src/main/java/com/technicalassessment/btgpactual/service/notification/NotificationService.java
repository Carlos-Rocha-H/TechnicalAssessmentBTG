package com.technicalassessment.btgpactual.service.notification;

import com.technicalassessment.btgpactual.model.Client;
import com.technicalassessment.btgpactual.model.Fund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final Map<String, NotificationSender> senders;

    public NotificationService(List<NotificationSender> senderList) {
        this.senders = senderList.stream()
                .collect(Collectors.toMap(NotificationSender::getType, Function.identity()));
    }

    public void notifySubscription(Client client, Fund fund) {
        String preference = client.getNotificationPreference();
        NotificationSender sender = senders.get(preference);

        if (sender == null) {
            log.warn("Preferencia de notificación no soportada: {} para cliente {}", preference, client.getClientId());
            return;
        }

        String destination = "EMAIL".equals(preference) ? client.getEmail() : client.getPhone();
        String subject = "Suscripción exitosa - " + fund.getName();
        String message = String.format(
                "Estimado %s, se ha realizado la suscripción al fondo %s por un monto de COP $%,.0f.",
                client.getName(), fund.getName(), fund.getMinimumAmount());

        sender.send(destination, subject, message);
    }
}
