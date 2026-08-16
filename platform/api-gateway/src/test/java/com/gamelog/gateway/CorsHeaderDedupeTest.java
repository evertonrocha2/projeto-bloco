package com.gamelog.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;

// Guarda contra um bug que so aparece NO NAVEGADOR.
//
// O monolito configura CORS por conta propria desde o TP1 (pro front na 5173
// conseguir chamar a 8080 direto). O gateway tambem configura CORS, porque e ele
// que o navegador enxerga agora. Resultado: as respostas das rotas do monolito
// saiam com Access-Control-Allow-Origin DUPLICADO.
//
// Duas ocorrencias do mesmo header e pior do que parece: o navegador nao soma os
// valores, ele RECUSA a resposta inteira ("contains multiple values, but only one
// is allowed"). O curl mostrava HTTP 200 e o JSON correto - e o catalogo nao
// carregava em tela nenhuma.
//
// A correcao e o filtro DedupeResponseHeader, que existe exatamente pra isso.
// Manter as duas configuracoes de CORS e proposital: assim continua valendo
// apontar o front direto pra 8080 (VITE_API_URL), sem a stack distribuida.
//
// Este teste afirma configuracao, e nao comportamento HTTP - o que e mais fraco
// do que o normal. Ainda vale: o defeito e invisivel fora do navegador, entao sem
// esta trava alguem removeria a linha por parecer supérflua e ninguem descobriria
// ate a tela parar de carregar.
@SpringBootTest
class CorsHeaderDedupeTest {

    @Autowired
    private GatewayProperties gatewayProperties;

    @Test
    @DisplayName("deduplica os headers de CORS que o monolito tambem envia")
    void dedupesCorsHeadersAlsoSentByTheMonolith() {
        assertThat(gatewayProperties.getDefaultFilters())
                .anySatisfy(filter -> {
                    assertThat(filter.getName()).isEqualTo("DedupeResponseHeader");
                    assertThat(filter.getArgs().values())
                            .anyMatch(value -> value.contains("Access-Control-Allow-Origin"));
                });
    }
}
