package com.gamelog.recommendation.messaging;

import com.gamelog.recommendation.projection.ActivityProjector;
import com.gamelog.recommendation.projection.ProjectionOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Consome os eventos do GameLog e mantem a projecao local em dia.
//
// E um metodo comum com @RabbitListener: o Spring AMQP cuida da conexao, do
// consumo, da conversao do JSON pro record, do ack quando o metodo retorna e do
// nack (com retentativa e DLQ) quando ele estoura. O codigo aqui so decide o que
// fazer com o evento.
@Component
public class GameLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(GameLogEventListener.class);

    private final ActivityProjector projector;
    private final RecalculationRequester recalculationRequester;

    public GameLogEventListener(ActivityProjector projector, RecalculationRequester recalculationRequester) {
        this.projector = projector;
        this.recalculationRequester = recalculationRequester;
    }

    @RabbitListener(id = "activity-events", queues = Messaging.ACTIVITY_QUEUE)
    public void onEvent(GameLogEvent event) {
        log.info("Evento recebido: {} ({}) de {}", event.eventType(), event.eventId(), event.aggregateId());

        // A projecao e atualizada (e commitada) ANTES de pedir o recalculo. O
        // comando so faz sentido depois que o dado novo esta la pra ser lido.
        ProjectionOutcome outcome = projector.apply(event);
        if (!outcome.duplicate()) {
            recalculationRequester.requestFor(outcome, event);
        }
    }
}
