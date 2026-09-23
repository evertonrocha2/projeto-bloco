package com.gamelog.recommendation.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Os payloads que este servico le, com SO os campos que usa.
//
// O evento review.created carrega o id da review e o titulo do jogo, que aqui nao
// servem pra nada - e nao aparecem nos records abaixo. Declarar menos do que o
// produtor manda e o que deixa o produtor evoluir: ele pode renomear um campo que
// ninguem aqui le sem quebrar este servico.
public final class EventPayloads {

    private EventPayloads() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewEvent(String username, Long gameId, String gameTitle, String genre, int rating) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CollectionEvent(String username, Long gameId, String gameTitle, String genre, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GameEvent(Long gameId, String title, String genre, String coverUrl) {
    }
}
