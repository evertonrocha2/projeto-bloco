package com.gamelog.recommendation.projection;

import com.gamelog.recommendation.messaging.GameLogEvent;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

// Evento num formato que este servico ainda nao sabe ler.
//
// Estende AmqpRejectAndDontRequeueException de proposito: o Spring AMQP entende
// essa excecao como "nao adianta tentar de novo", pula as retentativas e manda a
// mensagem direto pra DLQ. Tentar tres vezes um formato desconhecido daria o mesmo
// resultado tres vezes.
public class UnsupportedEventVersionException extends AmqpRejectAndDontRequeueException {

    public UnsupportedEventVersionException(GameLogEvent event) {
        super("Evento " + event.eventId() + " (" + event.eventType() + ") na versao "
                + event.schemaVersion() + "; este servico entende ate a versao "
                + ActivityProjector.SUPPORTED_SCHEMA_VERSION);
    }
}
