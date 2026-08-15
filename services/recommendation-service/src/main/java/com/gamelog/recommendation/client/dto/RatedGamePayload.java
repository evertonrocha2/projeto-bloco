package com.gamelog.recommendation.client.dto;

// Um jogo avaliado, do jeito que o monolito manda em
// /api/users/{username}/game-activity.
//
// Os nomes dos campos tem que casar EXATAMENTE com o JSON do outro servico - e
// por isso que este record existe separado do RatedGame do dominio. Se amanha o
// monolito renomear um campo, o ajuste acontece aqui e o algoritmo nao fica
// sabendo. Misturar as duas coisas amarraria o dominio ao formato de quem fornece
// o dado.
public record RatedGamePayload(Long gameId, String genre, int rating) {
}
