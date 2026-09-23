package com.gamelog.notification.messaging;

import com.gamelog.notification.service.NotificationProjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationProjector projector;

    public NotificationEventListener(NotificationProjector projector) {
        this.projector = projector;
    }

    @RabbitListener(id = "notification-events", queues = Messaging.EVENTS_QUEUE)
    public void onEvent(GameLogEvent event) {
        int created = projector.apply(event);
        log.info("Evento {} ({}) tratado: {} aviso(s) criado(s)", event.eventType(), event.eventId(), created);
    }
}
