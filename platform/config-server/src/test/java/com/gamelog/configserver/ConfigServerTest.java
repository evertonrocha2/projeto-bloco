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
}
