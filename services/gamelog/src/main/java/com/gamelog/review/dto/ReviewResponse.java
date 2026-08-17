package com.gamelog.review.dto;

import com.gamelog.review.domain.Review;

import java.time.Instant;

// Como uma review aparece pra fora. Repare que aqui a gente "achata" a entidade:
// em vez de mandar o objeto User/Game inteiro, manda so o que a tela precisa
// (username, titulo e capa do jogo). Assim nao vaza dado interno nem senha.
public record ReviewResponse(
        Long id,
        int rating,
        String text,
        Instant createdAt,
        String username,
        Long gameId,
        String gameTitle,
        String gameCoverUrl,

        // Votos e respostas. Nulo quando quem montou a resposta nao carregou a
        // camada social - o historico de revisoes, por exemplo, nao tem por que
        // pagar tres consultas pra dizer quantos polegares uma versao antiga
        // tinha.
        ReviewSocial social
) {
    // Fabrica que converte a entidade do banco no DTO da API. Sem social: quem
    // precisa dele carrega em lote e chama withSocial.
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getText(),
                review.getCreatedAt(),
                review.getUser().getUsername(),
                review.getGame().getId(),
                review.getGame().getTitle(),
                review.getGame().getCoverUrl(),
                null
        );
    }

    // Copia com a camada social presa.
    //
    // Separado da fabrica porque o social vem de uma consulta em LOTE, feita uma
    // vez pra pagina inteira. Se a fabrica o buscasse, cada review carregaria os
    // proprios votos - que e exatamente o N+1 que a consulta agrupada evita.
    public ReviewResponse withSocial(ReviewSocial social) {
        return new ReviewResponse(
                id, rating, text, createdAt, username, gameId, gameTitle, gameCoverUrl, social);
    }
}
