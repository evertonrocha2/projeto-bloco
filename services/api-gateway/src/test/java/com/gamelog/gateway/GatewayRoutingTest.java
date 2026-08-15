package com.gamelog.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

// Prova que o roteamento leva cada caminho ao servico certo.
//
// A armadilha que este teste protege: /api/** casa com /api/recommendations/**
// tambem. Se a rota generica do monolito fosse avaliada primeiro, TODA chamada de
// recomendacao iria pro monolito, que responderia 404 - e a tela de recomendacoes
// simplesmente nao funcionaria, sem nenhum erro de configuracao aparente.
@SpringBootTest
class GatewayRoutingTest {

    @Autowired
    private RouteLocator routeLocator;

    private Route routeById(String id) {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();
        return routes.stream()
                .filter(route -> route.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("rota nao encontrada: " + id));
    }

    private boolean matches(Route route, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(path).build());
        return Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block());
    }

    @Test
    @DisplayName("recomendacoes vao para o microsservico")
    void recommendationPathsGoToTheMicroservice() {
        Route route = routeById("recommendation-service");

        assertThat(matches(route, "/api/recommendations/demo")).isTrue();
        // E o destino e resolvido pelo NOME no Eureka, nao por host:porta fixo.
        assertThat(route.getUri().toString()).isEqualTo("lb://recommendation-service");
    }

    @Test
    @DisplayName("o resto da API vai para o monolito")
    void everythingElseGoesToTheMonolith() {
        Route route = routeById("gamelog-monolith");

        assertThat(matches(route, "/api/games")).isTrue();
        assertThat(matches(route, "/api/auth/login")).isTrue();
        assertThat(route.getUri().toString()).isEqualTo("lb://gamelog");
    }

    @Test
    @DisplayName("a rota de recomendacoes e avaliada antes da rota generica")
    void specificRouteIsEvaluatedBeforeTheCatchAll() {
        // Sem esta garantia, /api/recommendations/demo cairia na rota /api/** e
        // seria enviado pro monolito.
        assertThat(routeById("recommendation-service").getOrder())
                .isLessThan(routeById("gamelog-monolith").getOrder());
    }
}
