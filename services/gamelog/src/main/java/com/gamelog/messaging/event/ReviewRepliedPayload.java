package com.gamelog.messaging.event;

// Payload de review.replied.
//
// parentAuthor e o dono da resposta que foi respondida (nulo quando a resposta e
// direto na avaliacao). Quem notifica decide quem avisar; o evento so conta quem
// esta envolvido.
public record ReviewRepliedPayload(
        Long reviewId,
        Long replyId,
        String reviewAuthor,
        String replyAuthor,
        String parentAuthor,
        Long gameId,
        String gameTitle,
        String excerpt
) {
}
