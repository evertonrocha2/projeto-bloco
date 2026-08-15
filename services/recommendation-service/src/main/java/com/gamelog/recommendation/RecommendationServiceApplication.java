package com.gamelog.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// O microsservico de recomendacoes.
//
// Um processo separado, com banco separado e ciclo de vida separado do monolito.
// Sobe, cai e e implantado sozinho; a unica coisa que ele compartilha com o
// GameLog e o contrato HTTP.
//
// As tres anotacoes de Spring Cloud, e o que cada uma resolve:
//
//  @EnableDiscoveryClient - registra este servico no Eureka e permite encontrar
//      os outros pelo nome. Sem isso, o endereco do monolito seria uma constante
//      no codigo.
//
//  @EnableFeignClients - habilita os clientes HTTP declarativos (GameLogClient).
//      Junto com a descoberta, "lb://gamelog" vira um endereco real na hora da
//      chamada, com balanceamento entre instancias.
//
//  @ConfigurationPropertiesScan - encontra o ScoringProperties, que recebe os
//      pesos do algoritmo vindos do Config Server.
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ConfigurationPropertiesScan
public class RecommendationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationServiceApplication.class, args);
    }
}
