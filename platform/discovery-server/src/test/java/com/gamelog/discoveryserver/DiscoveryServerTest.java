package com.gamelog.discoveryserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Prova que o registro do Eureka esta de fato servindo, e nao so que o contexto
// do Spring carregou. /eureka/apps e o endpoint que os clientes consultam pra
// descobrir onde os outros servicos estao - se ele nao responde, a descoberta
// nao funciona, mesmo com a aplicacao "no ar".
//
// spring.cloud.config.enabled=false isola o teste: ele nao deve depender de um
// Config Server rodando na maquina de quem esta executando a suite.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.config.enabled=false")
class DiscoveryServerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("expoe o registro de servicos que os clientes consultam")
    void exposesServiceRegistry() {
        ResponseEntity<String> response = restTemplate.getForEntity("/eureka/apps", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("nao se registra em si mesmo")
    void doesNotRegisterWithItself() {
        // Recem-subido e sem clientes, o registro tem que estar vazio. Se o Eureka
        // tivesse se registrado nele mesmo, apareceria uma aplicacao aqui - foi
        // exatamente o risco que config-repo/discovery-server.yml evita.
        ResponseEntity<String> response = restTemplate.getForEntity("/eureka/apps", String.class);

        assertThat(response.getBody()).doesNotContain("DISCOVERY-SERVER");
    }
}
