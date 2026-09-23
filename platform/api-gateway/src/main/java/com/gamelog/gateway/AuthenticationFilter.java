package com.gamelog.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Exige token nas escritas do microsservico de recomendacoes e em todo o
// notification-service (exceto o feed publico).
//
// === Por que a checagem fica no gateway ===
//
// O microsservico NAO tem Spring Security e nao valida JWT. Isso e uma decisao,
// nao um esquecimento: replicar a validacao de token em cada servico significa
// espalhar a chave de assinatura e a logica de expiracao por todos eles. Como o
// gateway e o unico ponto de entrada externo, e nele que a barreira faz sentido.
//
// O monolito continua validando o token dele normalmente: as rotas /api/** passam
// por aqui, chegam la com o header Authorization intacto, e o
// JwtAuthenticationFilter dele faz a verificacao completa. Este filtro nao se mete
// nelas de proposito - duplicar a regra em dois lugares criaria duas versoes da
// verdade que podem divergir.
//
// === Limitacao assumida ===
//
// Aqui se confere a PRESENCA do header, nao a assinatura do token. Um token
// forjado passaria nas rotas de recomendacao. Pro escopo desta entrega e
// aceitavel - o dado protegido e feedback de recomendacao - mas nao seria em
// producao, e por isso esta escrito e nao escondido.
//
// A evolucao natural: o gateway virar um resource server OAuth2
// (spring-security-oauth2-resource-server) validando a assinatura uma vez pra
// todos os servicos de tras, ou um Authorization Server dedicado emitindo os
// tokens do sistema. Ficou fora do escopo porque exigiria refazer a autenticacao
// do monolito, que e assunto do TP1 e esta funcionando.
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final String RECOMMENDATIONS_PREFIX = "/api/recommendations";
    private static final String NOTIFICATIONS_PREFIX = "/api/notifications";
    private static final String PUBLIC_FEED = "/api/notifications/feed";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (requiresToken(request) && !hasBearerToken(request)) {
            log.warn("Requisicao sem token barrada no gateway: {} {}",
                    request.getMethod(), request.getPath());

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            // setComplete() encerra a resposta aqui: a cadeia nao continua e a
            // requisicao nunca chega ao servico de tras.
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    // Leitura de recomendacao e publica, igual ao perfil publico do monolito. O que
    // exige estar logado e ESCREVER: recalcular o lote de alguem, ou registrar
    // feedback no nome de alguem.
    //
    // Notificacoes (TP4) sao o contrario: ate a LEITURA e privada, porque a caixa
    // de entrada de alguem nao e publica. A excecao e o feed da comunidade, que so
    // mostra o que ja e publico (reviews publicadas). OPTIONS passa sempre: e o
    // preflight de CORS, que o navegador manda sem Authorization.
    private boolean requiresToken(ServerHttpRequest request) {
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();
        if (path.startsWith(NOTIFICATIONS_PREFIX)) {
            return method != HttpMethod.OPTIONS && !path.startsWith(PUBLIC_FEED);
        }
        return method == HttpMethod.POST && path.startsWith(RECOMMENDATIONS_PREFIX);
    }

    private boolean hasBearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith(BEARER_PREFIX);
    }

    // Roda antes dos filtros de roteamento: e mais barato recusar aqui do que
    // gastar uma chamada de rede pro servico de tras pra depois recusar.
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
