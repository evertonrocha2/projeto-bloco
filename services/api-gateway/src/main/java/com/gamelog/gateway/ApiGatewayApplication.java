package com.gamelog.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// API Gateway: a porta unica de entrada do sistema.
//
// Antes do TP3 o front falava direto com o monolito na 8080. Com dois servicos, o
// caminho ingenuo seria o front conhecer os dois enderecos - e ai cada servico
// novo exigiria mexer no front, cada servico precisaria da sua propria
// configuracao de CORS, e portas internas ficariam expostas ao navegador.
//
// Com o gateway, o front conhece um endereco (localhost:8090) e a divisao interna
// do sistema fica invisivel pra quem usa. O gateway decide, pelo caminho da URL,
// qual servico atende - e resolve o endereco pelo Eureka, nao por configuracao
// fixa. Como e o unico ponto de entrada externo, tambem e o lugar natural pra
// tratar assuntos transversais: CORS aqui, e a checagem de autenticacao das
// escritas no AuthenticationFilter.
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
