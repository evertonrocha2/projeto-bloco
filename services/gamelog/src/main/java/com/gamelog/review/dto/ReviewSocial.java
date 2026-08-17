package com.gamelog.review.dto;

import java.util.List;

// A camada social de UMA avaliacao: os dois placares, de que lado quem esta
// olhando ficou, e a arvore de respostas.
//
// Existe separado do ReviewResponse porque e carregado em LOTE, por uma consulta
// que atende a pagina inteira. Juntar tudo num DTO so faria cada ReviewResponse
// precisar buscar os proprios votos - que e exatamente o N+1 que a consulta
// agrupada existe pra evitar.
//
// myVote e String, e nao VoteType, pra JSON: null significa "nao votei" ou "nem
// estou logado", e um enum anulavel dentro do record so faria o controller
// converter na mao.
public record ReviewSocial(
        long positiveVotes,
        long negativeVotes,
        String myVote,
        List<ReplyResponse> replies
) {

    // Avaliacao sem voto e sem resposta. A tela precisa receber isso, e nao a
    // ausencia da chave: um card que le contagem de um nulo quebra.
    public static ReviewSocial empty() {
        return new ReviewSocial(0, 0, null, List.of());
    }
}
