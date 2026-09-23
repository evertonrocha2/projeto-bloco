package com.gamelog.messaging;

// Nomes dos recursos no RabbitMQ que o monolito conhece.
public final class Messaging {

    // Exchange topic onde saem todos os eventos de dominio. Topic, e nao fanout,
    // pra que cada consumidor assine so o que interessa a ele pela routing key.
    public static final String EVENTS_EXCHANGE = "gamelog.events";

    // Fila de pedidos de snapshot (request/reply). Quem pede manda a mensagem com
    // replyTo preenchido; o monolito responde direto pra esse endereco.
    public static final String SNAPSHOT_REQUEST_QUEUE = "gamelog.snapshot.requests";

    private Messaging() {
    }
}
