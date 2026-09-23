package com.gamelog.messaging.event;

// Payload de review.voted: alguem votou (ou trocou o voto) numa avaliacao.
// Desfazer o voto nao gera evento - ninguem quer ser avisado de que perdeu um
// polegar.
public record ReviewVotedPayload(
        Long reviewId,
        String reviewAuthor,
        String voter,
        String voteType,
        Long gameId,
        String gameTitle
) {
}
