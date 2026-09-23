package com.gamelog.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Topologia do RabbitMQ do lado do monolito.
//
// Exchanges e filas declarados como BEANS: o RabbitAdmin do Spring AMQP os cria no
// broker na primeira conexao, e a declaracao e idempotente - se ja existem com os
// mesmos argumentos, nada acontece. Nao ha script de setup pra rodar antes de
// subir o sistema, e nenhum servico depende de outro ter subido primeiro pra que o
// exchange exista: todos declaram o que usam.
//
// O monolito declara so o que e DELE: o exchange onde publica e a fila onde recebe
// pedidos. As filas de quem consome os eventos sao declaradas por cada consumidor
// - o produtor nao sabe (nem deve saber) quem esta ouvindo.
@Configuration
@EnableScheduling
public class MessagingConfig {

    @Bean
    public TopicExchange gameLogEventsExchange() {
        return ExchangeBuilder.topicExchange(Messaging.EVENTS_EXCHANGE).durable(true).build();
    }

    // Pedido de snapshot que ficou 30s sem ninguem atender nao e mais util: quem
    // pediu ja desistiu (o timeout de resposta dele e menor que isso). O TTL impede
    // que pedidos velhos se acumulem enquanto o monolito esta fora e sejam todos
    // respondidos de uma vez quando ele volta.
    @Bean
    public Queue snapshotRequestQueue() {
        return QueueBuilder.durable(Messaging.SNAPSHOT_REQUEST_QUEUE).ttl(30_000).build();
    }

    // JSON nas mensagens, usando o MESMO ObjectMapper do resto da aplicacao (datas
    // em ISO-8601, e nao em numero). O Spring Boot aplica este conversor
    // automaticamente no RabbitTemplate e nos @RabbitListener.
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
