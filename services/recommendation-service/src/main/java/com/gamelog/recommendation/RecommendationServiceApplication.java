package com.gamelog.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// O microsservico de recomendacoes.
//
// Um processo separado, com banco separado e ciclo de vida separado do monolito.
//
// TP4: a unica coisa que ele compartilha com o GameLog passou a ser o CONTRATO DOS
// EVENTOS no RabbitMQ. O @EnableFeignClients saiu: este servico nao faz mais
// nenhuma chamada HTTP ao monolito. Ele ouve o que acontece la (reviews, colecao,
// catalogo), guarda a parte que interessa numa projecao local e calcula as
// recomendacoes a partir dela.
//
//  @EnableDiscoveryClient - registra no Eureka pro gateway encontrar este servico.
//  @ConfigurationPropertiesScan - encontra o ScoringProperties (pesos do algoritmo
//      vindos do Config Server).
@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class RecommendationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationServiceApplication.class, args);
    }
}
