package com.gamelog.notification.messaging;

// Nomes da topologia RabbitMQ vista por este servico.
public final class Messaging {

    // Declarado pelo monolito tambem; declarar dos dois lados com os mesmos
    // atributos e idempotente e deixa qualquer um subir primeiro.
    public static final String EVENTS_EXCHANGE = "gamelog.events";

    public static final String EVENTS_QUEUE = "notification.events";
    public static final String DEAD_LETTER_EXCHANGE = "notification.dlx";
    public static final String EVENTS_DLQ = EVENTS_QUEUE + ".dlq";

    private Messaging() {
    }
}
