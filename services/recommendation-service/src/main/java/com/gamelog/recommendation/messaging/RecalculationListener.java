package com.gamelog.recommendation.messaging;

import com.gamelog.recommendation.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Executa os comandos de recalculo.
//
// concurrency "2-4": o container comeca com 2 consumidores nesta fila e sobe ate 4
// quando ela acumula. E o padrao competing consumers - cada comando vai pra UM
// consumidor so, e mais consumidores (ou mais replicas do servico) esvaziam a fila
// mais rapido. Diferente da fila de eventos, aqui a ordem nao importa: recalcular
// "ana" antes ou depois de "beto" da no mesmo.
//
// O recalculo e idempotente (apaga o lote e grava o novo a partir da projecao),
// entao um comando reentregue so custa processamento, nunca estado errado.
@Component
public class RecalculationListener {

    private static final Logger log = LoggerFactory.getLogger(RecalculationListener.class);

    private final RecommendationService recommendationService;

    public RecalculationListener(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @RabbitListener(id = "recalculate", queues = Messaging.RECALCULATE_QUEUE,
            concurrency = "${app.recalculation.concurrency:2-4}")
    public void onRecalculate(RecalculateCommand command) {
        var result = recommendationService.refresh(command.username());
        log.info("Recomendacoes de '{}' recalculadas ({} itens) por {} [causa {}]",
                command.username(), result.items().size(), command.reason(), command.causationId());
    }
}
