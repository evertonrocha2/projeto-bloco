package com.gamelog.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// Servidor de descoberta (Eureka).
//
// Sem ele, o microsservico de recomendacoes precisaria de "http://localhost:8080"
// escrito no codigo pra achar o monolito. Isso funciona na maquina do
// desenvolvedor e quebra em qualquer outro lugar - alem de impedir ter mais de
// uma instancia do mesmo servico.
//
// Com o Eureka, cada servico se registra pelo NOME ao subir, e quem chama pede
// pelo nome ("gamelog") em vez do endereco. O Spring Cloud LoadBalancer resolve
// o nome pra um endereco real na hora da chamada e distribui entre as instancias
// disponiveis. E o que permite subir, derrubar e mover servicos sem editar a
// configuracao de quem os consome.
//
// Painel com os servicos registrados: http://localhost:8761
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
