package com.gamelog.messaging.event;

import com.gamelog.review.domain.Review;

// Payload de review.created, review.updated e review.deleted.
//
// Leva genero e titulo do jogo junto com a nota. O microsservico de recomendacoes
// precisa do genero pra montar o perfil de gosto e o de notificacoes precisa do
// titulo pra escrever a mensagem; sem esses campos, cada um teria que perguntar ao
// monolito "que jogo e o 42?" a cada evento - e voltariamos a chamada sincrona.
//
// Em review.deleted o payload e o estado da review NO MOMENTO em que foi apagada.
// O consumidor precisa saber qual nota sair da media, nao so que algo sumiu.
public record ReviewEventPayload(
        Long reviewId,
        String username,
        Long gameId,
        String gameTitle,
        String genre,
        int rating
) {
    public static ReviewEventPayload from(Review review) {
        return new ReviewEventPayload(
                review.getId(),
                review.getUser().getUsername(),
                review.getGame().getId(),
                review.getGame().getTitle(),
                review.getGame().getGenre(),
                review.getRating());
    }
}
