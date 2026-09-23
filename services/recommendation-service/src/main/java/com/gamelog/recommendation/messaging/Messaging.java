package com.gamelog.recommendation.messaging;

// Nomes dos recursos no RabbitMQ que este servico usa.
public final class Messaging {

    // Declarado pelo monolito E por este servico, com os mesmos argumentos. A
    // declaracao e idempotente, e assim nenhum dos dois depende de o outro ter
    // subido primeiro pra que o exchange exista.
    public static final String EVENTS_EXCHANGE = "gamelog.events";

    // A fila DESTE servico no exchange de eventos. O nome leva o servico na frente
    // porque cada assinante tem a sua: e isso que faz o mesmo evento chegar aqui e
    // no notification-service, cada um no seu ritmo.
    public static final String ACTIVITY_QUEUE = "recommendation.activity-events";

    // Comandos internos: "recalcule as recomendacoes de fulano".
    public static final String COMMANDS_EXCHANGE = "recommendation.commands";
    public static final String RECALCULATE_QUEUE = "recommendation.recalculate";
    public static final String RECALCULATE_ROUTING_KEY = "recalculate";

    // Dead letter: pra onde vai a mensagem que falhou todas as tentativas.
    public static final String DEAD_LETTER_EXCHANGE = "recommendation.dlx";
    public static final String ACTIVITY_DLQ = ACTIVITY_QUEUE + ".dlq";
    public static final String RECALCULATE_DLQ = RECALCULATE_QUEUE + ".dlq";

    // Fila de pedidos de snapshot, declarada pelo monolito (que e quem responde).
    public static final String SNAPSHOT_REQUEST_QUEUE = "gamelog.snapshot.requests";

    private Messaging() {
    }
}
