package com.gamelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Ponto de entrada da aplicacao.
// @SpringBootApplication liga a autoconfiguracao do Spring Boot: ele varre o
// pacote com.gamelog procurando @Component/@Service/@Repository/@Controller e
// configura banco, web, seguranca etc. sozinho a partir das dependencias do pom.
@SpringBootApplication
public class GameLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameLogApplication.class, args);
    }
}
