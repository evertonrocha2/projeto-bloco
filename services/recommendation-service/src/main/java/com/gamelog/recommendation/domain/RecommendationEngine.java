package com.gamelog.recommendation.domain;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// O algoritmo de recomendacao.
//
// Estrategia: content-based por afinidade de genero. O perfil de gosto sai do que
// o usuario avaliou bem e do que tem na colecao; os candidatos sao os jogos do
// catalogo que ele ainda nao conhece; o score combina afinidade pessoal com a
// nota da comunidade.
//
// Por que nao filtragem colaborativa ("usuarios parecidos com voce gostaram de"):
// ela precisa de massa de dados pra funcionar. Com o volume de um banco de
// demonstracao, produziria recomendacoes pobres ou vazias. A abordagem por genero
// funciona a partir da PRIMEIRA avaliacao e, de bonus, e explicavel - da pra
// dizer ao usuario por que aquele jogo apareceu.
//
// Classe pura de proposito: nenhuma dependencia de Spring, banco ou rede. E o que
// permite testar todas as regras em milissegundos, sem subir nada.
public final class RecommendationEngine {

    public List<ScoredGame> recommend(GameActivity activity,
                                      List<CatalogGame> catalog,
                                      List<FeedbackEntry> feedback,
                                      ScoringWeights weights) {

        TasteProfile profile = TasteProfile.from(activity, catalog, feedback, weights);
        Set<Long> excluded = gamesToExclude(activity, feedback);

        return catalog.stream()
                .filter(game -> !excluded.contains(game.gameId()))
                .map(game -> score(game, profile, weights))
                .sorted(Comparator
                        .comparingDouble(ScoredGame::score).reversed()
                        // Desempate pelo id. Sem ele, jogos com score igual sairiam
                        // na ordem em que o catalogo chegou - a tela mudaria de ordem
                        // entre requisicoes sem nada ter mudado, e testes de ordem
                        // falhariam de forma intermitente.
                        .thenComparing(ScoredGame::gameId))
                .limit(weights.maxResults())
                .toList();
    }

    // Jogos que NAO podem ser recomendados.
    //
    // Esta e a parte da regra que mais protege a credibilidade da feature:
    // recomendar um jogo que a pessoa acabou de avaliar, ou que ela ja disse que
    // nao quer, faz o sistema parecer que nao presta atencao.
    private Set<Long> gamesToExclude(GameActivity activity, List<FeedbackEntry> feedback) {
        Set<Long> excluded = new HashSet<>();

        // Ja avaliou: conhece o jogo.
        activity.ratedGames().forEach(rated -> excluded.add(rated.gameId()));
        // Ja tem na colecao.
        excluded.addAll(activity.ownedGameIds());
        // Disse que nao interessa.
        feedback.stream()
                .filter(entry -> entry.verdict() == FeedbackVerdict.DISMISSED)
                .forEach(entry -> excluded.add(entry.gameId()));

        return excluded;
    }

    // score = afinidade * genreWeight + (notaDaComunidade / 5) * communityWeight
    //
    // Com os pesos padrao (3.0 e 2.0) o maximo e 5.0. Dividir a nota por 5
    // normaliza as duas componentes pra mesma escala 0..1 antes de aplicar os
    // pesos - senao a nota da comunidade, que vai de 0 a 5, esmagaria a afinidade,
    // que vai de 0 a 1.
    private ScoredGame score(CatalogGame game, TasteProfile profile, ScoringWeights weights) {
        double affinity = profile.affinityFor(game.genre());
        double community = game.averageRating() / 5.0;

        double score = affinity * weights.genreWeight() + community * weights.communityWeight();

        // Se nao ha afinidade, nao ha genero a citar: a recomendacao veio da nota
        // da comunidade, e a tela diz isso em vez de inventar um motivo pessoal.
        List<String> reasonGenres = affinity > 0.0
                ? profile.strongestGenresIn(game.genre(), 2)
                : List.of();

        return new ScoredGame(
                game.gameId(),
                game.title(),
                game.coverUrl(),
                round(score),
                reasonGenres);
    }

    // Duas casas decimais: o score aparece na tela, e 3.3000000000000003 nao
    // ajuda ninguem. Arredondar aqui tambem deixa o valor persistido estavel.
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
