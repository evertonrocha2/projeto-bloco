package com.gamelog.recommendation.client;

import java.util.Optional;

// De onde vem o retrato do GameLog.
//
// Esta interface e a fronteira entre "calcular recomendacoes" e "falar com outro
// servico pela rede". O RecommendationService depende dela, nao do Feign - o
// mesmo principio de inversao de dependencia que o monolito aplica com as
// interfaces de repositorio.
//
// O ganho pratico aparece em duas frentes:
//
//  1. Nos testes: da pra colocar um duplo de dez linhas no lugar do monolito e
//     exercitar "servico fora do ar" sem circuit breaker, sem rede, sem Spring.
//
//  2. No desenho: quem chama nao precisa saber que existe HTTP, Feign, Eureka ou
//     disjuntor no meio. Trocar Feign por RestClient, ou passar a receber os dados
//     por mensageria, nao mexe em uma linha do algoritmo.
//
// Optional.empty() significa "nao foi possivel obter o retrato" - normalmente o
// monolito fora do ar ou o disjuntor aberto. Nao e excecao porque, aqui, nao
// conseguir falar com o outro servico e um cenario PREVISTO, com plano B
// definido: servir o ultimo lote gravado. Excecao seria tratar isso como
// imprevisto.
public interface ActivitySource {

    Optional<GameLogSnapshot> fetch(String username);
}
