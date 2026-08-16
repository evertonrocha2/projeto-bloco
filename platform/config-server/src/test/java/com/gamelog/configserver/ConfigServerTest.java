package com.gamelog.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

// Estes testes nao verificam so que a aplicacao sobe: eles provam que o servidor
// realmente ENTREGA a configuracao lida do config-repo. E o comportamento que
// importa - um Config Server que sobe mas serve um mapa vazio passaria num teste
// de "contexto carrega" e quebraria todo mundo em producao.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("serve os pesos do algoritmo pro microsservico de recomendacoes")
    void servesScoringWeightsToRecommendationService() {
        // O contrato do Config Server: GET /{aplicacao}/{perfil}
        String body = restTemplate.getForObject("/recommendation-service/default", String.class);

        assertThat(body)
                .contains("recommendation.scoring.genre-weight")
                .contains("recommendation.scoring.max-results");
    }

    @Test
    @DisplayName("serve a configuracao comum (Eureka) pra qualquer servico que pedir")
    void servesSharedConfigurationToAnyService() {
        // "application.yml" no config-repo vale pra todos os clientes, por isso um
        // nome de servico inventado tambem tem que receber o endereco do Eureka.
        String body = restTemplate.getForObject("/servico-que-nao-existe/default", String.class);

        assertThat(body).contains("eureka.client.service-url.defaultZone");
    }

    @Test
    @DisplayName("o arquivo do servico devolve os endpoints extras que ele precisa")
    void perServiceFileCarriesTheEndpointsThatServiceNeeds() {
        // A hierarquia de precedencia morde aqui. O application.yml compartilhado
        // manda "include: health,info,refresh,circuitbreakers" pra TODO cliente, e
        // configuracao do servidor tem prioridade sobre o application.yml local do
        // servico. Resultado: a lista local do gateway (que inclui "gateway") era
        // descartada, e /actuator/gateway/routes respondia 404 - sem nada na
        // configuracao parecer errado, porque o arquivo local estava certo.
        //
        // A correcao e o arquivo com o NOME DO SERVICO, que vence o compartilhado.
        // Mesmo padrao do discovery-server.yml.
        //
        // Este teste vive no Config Server, e nao no gateway, porque e o unico lugar
        // que enxerga o problema: nos testes do gateway o Config Server esta
        // desligado, entao la o arquivo local sempre parece correto.
        String body = restTemplate.getForObject("/api-gateway/default", String.class);

        // Afirma que o ARQUIVO especifico do servico foi servido, e nao apenas que a
        // string "gateway" aparece em algum lugar - ela apareceria de qualquer jeito,
        // porque a resposta ecoa o nome da aplicacao pedida ("api-gateway"). Uma
        // assercao frouxa aqui passaria mesmo sem o arquivo existir.
        assertThat(body).contains("api-gateway.yml");
        assertThat(body).contains("management.endpoints.web.exposure.include");
    }
}
