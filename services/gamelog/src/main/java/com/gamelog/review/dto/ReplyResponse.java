package com.gamelog.review.dto;

import com.gamelog.review.domain.ReviewReply;
import java.time.Instant;
import java.util.List;

// Uma resposta como a tela a recebe, ja com as filhas dentro.
//
// A arvore vem montada do servidor, e nao plana com parentId pra tela remontar:
// a regra de "quem e filho de quem" e uma so, e deixa-la no cliente significaria
// reescreve-la em qualquer outro consumidor da API.
//
// deleted vem separado de text porque a tela precisa distinguir dois casos que
// tem o mesmo texto vazio: a resposta apagada, que vira "[removido]" e mantem o
// lugar na conversa, e a resposta que nunca existiu.
public record ReplyResponse(
        Long id,
        String username,
        String text,
        boolean deleted,
        int depth,
        Instant createdAt,
        List<ReplyResponse> children
) {

    public static ReplyResponse from(ReviewReply reply, List<ReplyResponse> children) {
        return new ReplyResponse(
                reply.getId(),
                reply.getUser().getUsername(),
                reply.getText(),
                reply.isDeleted(),
                reply.getDepth(),
                reply.getCreatedAt(),
                children
        );
    }
}
