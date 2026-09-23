package com.gamelog.recommendation.projection;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

// Mensagem que nunca vai dar certo: payload que nao casa com o formato esperado.
// Mesmo raciocinio da UnsupportedEventVersionException - vai direto pra DLQ, sem
// gastar retentativas.
public class InvalidEventException extends AmqpRejectAndDontRequeueException {

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
