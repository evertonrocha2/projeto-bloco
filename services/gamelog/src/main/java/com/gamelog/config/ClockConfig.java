package com.gamelog.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    // O relogio da aplicacao, como dependencia injetavel.
    //
    // Quem precisa saber "que horas sao" recebe isto no construtor em vez de
    // chamar Instant.now() no meio do metodo. A diferenca aparece no teste: a
    // retrospectiva do ano do perfil so seria testavel no ano em que o teste foi
    // escrito, e comecaria a falhar sozinha na virada - um teste que quebra sem
    // ninguem ter mexido em nada e um teste que a equipe aprende a ignorar.
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
