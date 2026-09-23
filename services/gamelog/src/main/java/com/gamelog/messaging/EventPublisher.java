package com.gamelog.messaging;

// Porta de saida dos eventos de dominio.
//
// Os services de negocio dependem desta interface, e nao do RabbitTemplate. Mesma
// inversao de dependencia do ActivitySource no microsservico: o ReviewService diz
// "aconteceu isto" e nao sabe se o fato vai pra uma tabela de outbox, direto pro
// broker ou pra um duplo de teste.
//
// Quem chama tem que estar dentro de uma transacao de negocio. A implementacao
// padrao grava o evento NA MESMA transacao do dado (ver OutboxEventPublisher), e e
// isso que garante que evento e dado nunca divergem.
public interface EventPublisher {

    void publish(String eventType, String aggregateId, Object payload);
}
