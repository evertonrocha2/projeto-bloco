package com.gamelog.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

// Servidor de configuracao centralizada (Spring Cloud Config).
//
// O problema que ele resolve: com cinco aplicacoes rodando, um valor como o
// endereco do Eureka apareceria repetido em cinco arquivos. Mudar uma coisa
// viraria cinco edicoes, e a chance de esquecer uma seria alta. Aqui a
// configuracao mora num lugar so (config-repo/) e cada servico busca a dele
// por HTTP durante o startup.
//
// O ganho maior nem e evitar repeticao: e conseguir MUDAR configuracao de um
// servico que esta no ar. O microsservico de recomendacoes usa @RefreshScope nos
// pesos do algoritmo, entao editar o .yml aqui e chamar /actuator/refresh la
// altera o comportamento sem recompilar e sem reiniciar nada.
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
