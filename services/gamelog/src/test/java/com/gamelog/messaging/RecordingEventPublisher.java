package com.gamelog.messaging;

import java.util.ArrayList;
import java.util.List;

// Duplo do EventPublisher para testes de service: so anota o que foi publicado.
public class RecordingEventPublisher implements EventPublisher {

    public record Published(String eventType, String aggregateId, Object payload) {
    }

    private final List<Published> published = new ArrayList<>();

    @Override
    public void publish(String eventType, String aggregateId, Object payload) {
        published.add(new Published(eventType, aggregateId, payload));
    }

    public List<Published> published() {
        return published;
    }

    public List<String> types() {
        return published.stream().map(Published::eventType).toList();
    }
}
