package com.gamelog.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

// O gateway barra escrita sem token nas rotas do microsservico.
//
// Precisa de teste porque a regra tem quatro caminhos faceis de errar, e errar
// pra qualquer lado e ruim: barrar demais quebra o login e o catalogo; barrar de
// menos deixa as escritas do microsservico abertas, ja que ele nao tem Spring
// Security proprio.
class AuthenticationFilterTest {

    private final AuthenticationFilter filter = new AuthenticationFilter();

    // Cadeia de teste que so anota se foi chamada. Se o filtro barrou, ela nao roda.
    private static class RecordingChain implements GatewayFilterChain {
        private final AtomicBoolean called = new AtomicBoolean(false);

        @Override
        public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange) {
            called.set(true);
            return Mono.empty();
        }

        boolean wasCalled() {
            return called.get();
        }
    }

    @Test
    @DisplayName("POST sem token e barrado com 401 e nao chega ao servico")
    void writeWithoutTokenIsRejected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/recommendations/demo/refresh").build());
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Barrar aqui evita gastar uma chamada de rede pro servico de tras.
        assertThat(chain.wasCalled()).isFalse();
    }

    @Test
    @DisplayName("POST com token Bearer segue para o servico")
    void writeWithBearerTokenPassesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/recommendations/demo/feedback")
                        .header("Authorization", "Bearer um-token-qualquer")
                        .build());
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isTrue();
    }

    @Test
    @DisplayName("leitura de recomendacoes continua publica")
    void readingRecommendationsStaysPublic() {
        // Ver recomendacoes de alguem e publico, igual ao perfil publico do monolito.
        // Exigir token na leitura quebraria a tela de quem nao esta logado.
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/recommendations/demo").build());
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isTrue();
    }

    @Test
    @DisplayName("nao interfere nas escritas do monolito")
    void doesNotInterfereWithMonolithWrites() {
        // Publicar review passa por aqui a caminho do monolito, que tem o proprio
        // JwtAuthenticationFilter e faz a validacao COMPLETA do token (assinatura e
        // expiracao). Barrar aqui tambem seria pior, nao melhor: duplicaria a regra
        // em dois lugares que poderiam divergir. Este filtro cobre so o servico que
        // nao tem guarda propria.
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/games/1/reviews").build());
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isTrue();
    }
}
