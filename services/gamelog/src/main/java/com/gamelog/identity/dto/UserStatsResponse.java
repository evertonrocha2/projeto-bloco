package com.gamelog.identity.dto;

import java.util.List;
import java.util.Map;

// Os numeros do perfil: a faixa do topo, as conquistas e a retrospectiva do ano.
//
// Um endpoint so pra tudo isso. A alternativa seria a tela de perfil abrir cinco
// requisicoes ao carregar - e as cinco leem exatamente as mesmas duas tabelas.
//
// Nada aqui e guardado: tudo e derivado na leitura. Contadores persistidos
// criariam a classe de bug mais chata que existe - o numero que discorda da lista
// logo abaixo dele, porque um caminho de escrita esqueceu de incrementar.
public record UserStatsResponse(

        // ----- a estante em numeros -----
        int totalHours,

        // Codigo do status -> quantos jogos. Sempre com os cinco status, mesmo os
        // zerados: as abas do perfil mostram todos, e uma chave ausente faria a
        // aba renderizar "undefined" no lugar do numero.
        Map<String, Long> countByStatus,

        String favoriteGenre,

        double averageRatingGiven,

        // ----- retrospectiva -----
        int year,
        int finishedThisYear,
        String mostPlayedThisYear,
        String bestRatedThisYear,

        // ----- conquistas -----
        // So os codigos. O rotulo e a descricao moram no front, num modulo puro:
        // assim o texto muda sem tocar no servidor, e o calculo de "quais estao
        // ganhas" fica testavel sem banco.
        List<String> achievements
) {
}
