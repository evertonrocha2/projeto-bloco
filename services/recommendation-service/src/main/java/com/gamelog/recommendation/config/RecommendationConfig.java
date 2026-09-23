package com.gamelog.recommendation.config;

import com.gamelog.recommendation.domain.RecommendationEngine;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Registra o motor de recomendacao como bean.
//
// Por que aqui e nao um @Component na propria classe: o RecommendationEngine e
// dominio puro, e manter dominio livre de anotacoes de framework e o que permite
// instancia-lo num teste com "new RecommendationEngine()", sem contexto do Spring.
// A decisao de que ele participa da injecao de dependencias e da aplicacao, nao do
// dominio - entao mora na camada de configuracao.
@Configuration
public class RecommendationConfig {

    @Bean
    public RecommendationEngine recommendationEngine() {
        return new RecommendationEngine();
    }

    // Relogio injetavel: a projecao compara instantes de eventos, e os testes
    // precisam controlar "agora" pra exercitar evento atrasado.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
