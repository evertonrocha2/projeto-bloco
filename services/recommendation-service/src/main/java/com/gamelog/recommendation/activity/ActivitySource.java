package com.gamelog.recommendation.activity;

import java.util.Optional;

// De onde vem o retrato do GameLog.
//
// Esta interface e a fronteira entre "calcular recomendacoes" e "obter os dados de
// outro servico". O RecommendationService depende dela, nao de como o dado chega.
//
// No TP3 a implementacao era um cliente Feign que chamava o monolito a cada
// recalculo, protegido por circuit breaker. O comentario daquela versao dizia que
// "passar a receber os dados por mensageria nao mexe em uma linha do algoritmo" -
// e no TP4 foi exatamente o que aconteceu: a implementacao virou
// ProjectionActivitySource, que le uma copia local mantida pelos eventos, e nem o
// RecommendationService nem o RecommendationEngine mudaram de assinatura.
//
// Optional.empty() significa "ainda nao ha retrato confiavel" - a projecao local
// ainda nao recebeu o snapshot inicial do monolito. A tela recebe as recomendacoes
// gravadas marcadas como desatualizadas, em vez de um erro.
public interface ActivitySource {

    Optional<GameLogSnapshot> fetch(String username);
}
