package com.gamelog.messaging;

// Os eventos que o monolito publica, e o nome de cada um no broker.
//
// O tipo do evento e TAMBEM a routing key no exchange topic "gamelog.events". Um
// nome so pras duas coisas evita a classe de erro em que o evento diz ser
// "review.created" mas foi roteado como outra coisa.
//
// A convencao <agregado>.<fato no passado> nao e estetica: e ela que deixa os
// consumidores assinarem por padrao. "review.*" pega tudo de review; "#.created"
// pegaria toda criacao de qualquer agregado. Um nome como "createReview" (verbo no
// imperativo) seria um COMANDO, outra coisa - ver docs/EVENTOS.md.
public final class EventTypes {

    public static final String REVIEW_CREATED = "review.created";
    public static final String REVIEW_UPDATED = "review.updated";
    public static final String REVIEW_DELETED = "review.deleted";
    public static final String REVIEW_REPLIED = "review.replied";
    public static final String REVIEW_VOTED = "review.voted";
    public static final String COLLECTION_UPDATED = "collection.updated";
    public static final String GAME_ADDED = "catalog.game.added";
    public static final String USER_REGISTERED = "user.registered";

    private EventTypes() {
    }
}
