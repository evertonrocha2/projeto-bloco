package com.gamelog.recommendation.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.scheduling.annotation.EnableScheduling;

// Topologia RabbitMQ do servico de recomendacoes.
//
//   gamelog.events (topic) --review.*------------> recommendation.activity-events
//                          --collection.*--------/        | falhou 3x
//                          --catalog.#-----------/        v
//                                                 recommendation.dlx --> *.dlq
//
//   recommendation.commands (direct) --recalculate--> recommendation.recalculate
//
// Dois padroes de consumo diferentes, de proposito:
//
//  - A fila de EVENTOS tem consumidor ativo unico (x-single-active-consumer). A
//    projecao depende da ordem dos eventos, e com dois consumidores em paralelo
//    "review apagada" poderia ser aplicada antes de "review criada". Com varias
//    replicas do servico no Kubernetes, o RabbitMQ entrega pra UMA delas; se ela
//    cair, outra assume sozinha. Escala de disponibilidade, nao de vazao.
//
//  - A fila de COMANDOS tem consumidores concorrentes (competing consumers). Cada
//    recalculo e independente dos outros, entao 4 consumidores por replica, vezes
//    N replicas, dividem a fila. Aqui escala de vazao e o objetivo.
@Configuration
@EnableScheduling
public class RabbitMessagingConfig {

    @Bean
    public TopicExchange gameLogEventsExchange() {
        return ExchangeBuilder.topicExchange(Messaging.EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange recommendationCommandsExchange() {
        return ExchangeBuilder.directExchange(Messaging.COMMANDS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange recommendationDeadLetterExchange() {
        return ExchangeBuilder.directExchange(Messaging.DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue activityEventsQueue() {
        return QueueBuilder.durable(Messaging.ACTIVITY_QUEUE)
                .deadLetterExchange(Messaging.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(Messaging.ACTIVITY_DLQ)
                .singleActiveConsumer()
                .build();
    }

    @Bean
    public Queue recalculateQueue() {
        return QueueBuilder.durable(Messaging.RECALCULATE_QUEUE)
                .deadLetterExchange(Messaging.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(Messaging.RECALCULATE_DLQ)
                .build();
    }

    @Bean
    public Queue activityDeadLetterQueue() {
        return QueueBuilder.durable(Messaging.ACTIVITY_DLQ).build();
    }

    @Bean
    public Queue recalculateDeadLetterQueue() {
        return QueueBuilder.durable(Messaging.RECALCULATE_DLQ).build();
    }

    // As assinaturas. E AQUI que este servico diz o que quer ouvir; o monolito nao
    // participa dessa decisao.
    @Bean
    public Declarables recommendationBindings(TopicExchange gameLogEventsExchange,
                                              DirectExchange recommendationCommandsExchange,
                                              DirectExchange recommendationDeadLetterExchange,
                                              Queue activityEventsQueue,
                                              Queue recalculateQueue,
                                              Queue activityDeadLetterQueue,
                                              Queue recalculateDeadLetterQueue) {
        return new Declarables(
                bind(activityEventsQueue, gameLogEventsExchange, "review.*"),
                bind(activityEventsQueue, gameLogEventsExchange, "collection.*"),
                bind(activityEventsQueue, gameLogEventsExchange, "catalog.#"),
                BindingBuilder.bind(recalculateQueue).to(recommendationCommandsExchange)
                        .with(Messaging.RECALCULATE_ROUTING_KEY),
                BindingBuilder.bind(activityDeadLetterQueue).to(recommendationDeadLetterExchange)
                        .with(Messaging.ACTIVITY_DLQ),
                BindingBuilder.bind(recalculateDeadLetterQueue).to(recommendationDeadLetterExchange)
                        .with(Messaging.RECALCULATE_DLQ));
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // A fabrica dos @RabbitListener, com a politica de falha:
    //
    //  - erro transitorio (banco ocupado, deadlock): tenta de novo ate 3 vezes, com
    //    espera crescente (0,5s, 1s, 2s). A maioria resolve sozinha na segunda.
    //  - erro permanente (payload invalido, versao desconhecida): nao retenta. Tres
    //    tentativas com a mesma mensagem quebrada dariam o mesmo erro tres vezes.
    //  - esgotou: RejectAndDontRequeueRecoverer rejeita SEM devolver pra fila, e o
    //    RabbitMQ manda a mensagem pro dead letter exchange configurado na fila.
    //
    // Sem a DLQ, as opcoes seriam perder a mensagem (ack) ou devolve-la pra fila
    // (requeue) - e uma mensagem venenosa devolvida volta pra frente da fila e
    // trava todas as outras atras dela pra sempre.
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Value("${app.messaging.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.messaging.retry.initial-interval-ms:500}") long initialInterval) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts, Map.of(
                AmqpRejectAndDontRequeueException.class, false,
                MessageConversionException.class, false),
                // traverseCauses: o listener embrulha a excecao original; a politica
                // precisa olhar a causa pra reconhecer o erro permanente.
                true,
                // retryable por padrao: o que nao esta no mapa e considerado transitorio.
                true);

        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .retryPolicy(retryPolicy)
                .backOffOptions(initialInterval, 2.0, 5_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    private static Binding bind(Queue queue, TopicExchange exchange, String pattern) {
        return BindingBuilder.bind(queue).to(exchange).with(pattern);
    }
}
