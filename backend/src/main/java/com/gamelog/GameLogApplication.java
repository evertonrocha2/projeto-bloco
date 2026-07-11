package com.gamelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Ponto de entrada da aplicacao.
// @SpringBootApplication liga a autoconfiguracao do Spring Boot: ele varre o
// pacote com.gamelog procurando @Component/@Service/@Repository/@Controller e
// configura banco, web, seguranca etc. sozinho a partir das dependencias do pom.
//
// @EnableJpaAuditing liga o preenchimento automatico de @CreatedDate e
// @LastModifiedDate (ver a superclasse Auditable).
//
// @EnableJpaRepositories com o factory bean do Envers permite que os
// repositorios tambem estendam RevisionRepository e consultem o historico
// de revisoes das entidades auditadas.
@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(
        basePackages = "com.gamelog",
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class
)
public class GameLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameLogApplication.class, args);
    }
}
