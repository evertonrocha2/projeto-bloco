package com.gamelog.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
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

// Topologia do servico de notificacoes.
//
//   gamelog.events (topic) --review.created----> notification.events
//                          --review.deleted---/        | falhou 3x
//                          --review.replied---/        v
//                          --review.voted-----/  notification.dlx --> notification.events.dlq
//                          --user.registered--/
//
// A mesma publicacao de "review.created" que alimenta a projecao de recomendacoes
// cai tambem nesta fila: cada assinante tem a SUA fila, e o exchange copia a
// mensagem pra todas as que casam com a routing key. E isso que torna o
// publish/subscribe diferente de uma fila de trabalho.
//
// Aqui as assinaturas sao nomes exatos, e nao "review.*": notificacao so quer o
// que sabe tratar. Um review.updated (nota editada) nao vira aviso pra ninguem,
// entao nem vale a pena recebe-lo.
@Configuration
@EnableScheduling
public class RabbitMessagingConfig {

    static final String[] ROUTING_KEYS = {
            "review.created", "review.deleted", "review.replied", "review.voted", "user.registered"};

    @Bean
    public TopicExchange gameLogEventsExchange() {
        return ExchangeBuilder.topicExchange(Messaging.EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange notificationDeadLetterExchange() {
        return ExchangeBuilder.directExchange(Messaging.DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    // Consumidor ativo unico pelo mesmo motivo da fila de atividade das
    // recomendacoes: "review.deleted" precisa ser aplicado depois de
    // "review.created", senao o item do feed ressuscita.
    @Bean
    public Queue notificationEventsQueue() {
        return QueueBuilder.durable(Messaging.EVENTS_QUEUE)
                .deadLetterExchange(Messaging.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(Messaging.EVENTS_DLQ)
                .singleActiveConsumer()
                .build();
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(Messaging.EVENTS_DLQ).build();
    }

    @Bean
    public Declarables notificationBindings(TopicExchange gameLogEventsExchange,
                                            DirectExchange notificationDeadLetterExchange,
                                            Queue notificationEventsQueue,
                                            Queue notificationDeadLetterQueue) {
        List<Declarable> bindings = new ArrayList<>();
        bindings.add(BindingBuilder.bind(notificationDeadLetterQueue).to(notificationDeadLetterExchange)
                .with(Messaging.EVENTS_DLQ));
        for (String key : ROUTING_KEYS) {
            bindings.add(BindingBuilder.bind(notificationEventsQueue).to(gameLogEventsExchange).with(key));
        }
        return new Declarables(bindings);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // Mesma politica de falha do recommendation-service: retenta o transitorio,
    // manda o permanente direto pra DLQ.
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
                MessageConversionException.class, false), true, true);

        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .retryPolicy(retryPolicy)
                .backOffOptions(initialInterval, 2.0, 5_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
