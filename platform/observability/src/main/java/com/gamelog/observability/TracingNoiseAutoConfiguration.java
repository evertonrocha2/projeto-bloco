package com.gamelog.observability;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Tira do Zipkin o trafego que nao e de negocio.
//
// Com amostragem de 100%, o que mais gera trace num sistema parado nao e usuario:
// e o Prometheus raspando /actuator/prometheus a cada 15 s, o Kubernetes batendo
// nas probes a cada 10 s e o cliente do Eureka mandando heartbeat a cada 30 s -
// em cada instancia. Sem filtro, a tela do Zipkin fica tomada por esses traces e
// o POST que se quer investigar some no meio.
//
// O filtro atua na OBSERVATION, entao a requisicao de actuator tambem nao vira
// metrica http.server.requests - de proposito: a latencia do scrape nao e a
// latencia que o usuario sente, e misturar as duas distorce o p95 do painel.
@AutoConfiguration
@ConditionalOnClass(ObservationPredicate.class)
public class TracingNoiseAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "org.springframework.http.client.observation.ClientRequestObservationContext")
    ObservationPredicate ignoreEurekaClientCalls() {
        return TracingNoiseFilters::notEurekaClientCall;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class Servlet {
        @Bean
        ObservationPredicate ignoreActuatorServletRequests() {
            return TracingNoiseFilters::notServletActuatorRequest;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    static class Reactive {
        @Bean
        ObservationPredicate ignoreActuatorReactiveRequests() {
            return TracingNoiseFilters::notReactiveActuatorRequest;
        }
    }
}
