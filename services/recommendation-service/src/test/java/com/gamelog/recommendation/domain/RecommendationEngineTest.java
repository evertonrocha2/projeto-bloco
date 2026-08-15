package com.gamelog.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// O motor de recomendacao: recebe atividade + catalogo + feedback e devolve a
// lista pontuada. Puro, sem Spring nem banco.
//
// E a classe mais importante do microsservico, e a que concentra mais testes:
// aqui moram as regras que decidem o que o usuario ve. As excluioes (jogo ja
// avaliado, jogo da colecao, jogo descartado) sao tao importantes quanto a
// pontuacao - recomendar algo que a pessoa ja tem destroi a confianca na feature
// mais rapido do que uma recomendacao mediana.
class RecommendationEngineTest {

    private final RecommendationEngine engine = new RecommendationEngine();

    private ScoringWeights weights() {
        return new ScoringWeights(3, 0.5, 1.5, 3.0, 2.0, 8);
    }

    // Catalogo fixo dos testes. As notas da comunidade sao diferentes de proposito
    // pra dar pra observar a ordenacao.
    private List<CatalogGame> catalog() {
        return List.of(
                new CatalogGame(1L, "Elden Ring", "url1", "Action, RPG", 4.5),
                new CatalogGame(2L, "FIFA", "url2", "Sports", 3.0),
                new CatalogGame(3L, "Hades", "url3", "Roguelike, Indie", 4.8),
                new CatalogGame(4L, "Celeste", "url4", "Indie, Platformer", 4.2));
    }

    // Perfil de quem gosta de RPG. O jogo avaliado (99) fica FORA do catalogo pra
    // nao interferir na lista de candidatos.
    private GameActivity gostaDeRpg() {
        return new GameActivity("ana", List.of(new RatedGame(99L, "RPG", 5)), List.of());
    }

    @Test
    @DisplayName("o jogo do genero preferido vem primeiro")
    void gameMatchingFavouriteGenreRanksFirst() {
        // Elden Ring ("Action, RPG") tem afinidade 0.5 pra quem gosta de RPG, o que
        // lhe da 1.5 de genero + 1.8 de comunidade = 3.3. Hades, sem afinidade, fica
        // com 1.92 apesar de ter nota media MAIOR. E o comportamento desejado: a
        // recomendacao e pessoal, nao um ranking de popularidade.
        List<ScoredGame> result = engine.recommend(
                gostaDeRpg(), catalog(), List.of(), weights());

        assertThat(result.get(0).gameId()).isEqualTo(1L);
        assertThat(result.get(0).score()).isGreaterThan(result.get(1).score());
    }

    @Test
    @DisplayName("nao recomenda jogo que o usuario ja avaliou")
    void doesNotRecommendAlreadyRatedGame() {
        GameActivity activity = new GameActivity("ana",
                List.of(new RatedGame(1L, "Action, RPG", 5)),
                List.of());

        List<ScoredGame> result = engine.recommend(activity, catalog(), List.of(), weights());

        assertThat(result).noneMatch(game -> game.gameId().equals(1L));
    }

    @Test
    @DisplayName("nao recomenda jogo que ja esta na colecao")
    void doesNotRecommendGameAlreadyInCollection() {
        GameActivity activity = new GameActivity("ana",
                List.of(new RatedGame(99L, "RPG", 5)),
                List.of(3L));

        List<ScoredGame> result = engine.recommend(activity, catalog(), List.of(), weights());

        assertThat(result).noneMatch(game -> game.gameId().equals(3L));
    }

    @Test
    @DisplayName("jogo descartado nao volta a aparecer")
    void dismissedGameNeverComesBack() {
        // Sem isso, o usuario descartaria o mesmo jogo pra sempre - o "nao me
        // interessa" nao teria efeito nenhum e a feature pareceria quebrada.
        List<ScoredGame> result = engine.recommend(gostaDeRpg(), catalog(),
                List.of(new FeedbackEntry(1L, FeedbackVerdict.DISMISSED)), weights());

        assertThat(result).noneMatch(game -> game.gameId().equals(1L));
    }

    @Test
    @DisplayName("usuario sem atividade recebe os melhores avaliados da comunidade")
    void userWithoutActivityGetsCommunityFavourites() {
        // Cold start. Devolver lista vazia pra quem acabou de se cadastrar seria a
        // pior primeira impressao possivel, entao sem perfil o score cai so na
        // componente de comunidade e a ordem passa a ser por nota media.
        List<ScoredGame> result = engine.recommend(
                GameActivity.empty("novato"), catalog(), List.of(), weights());

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).gameId()).isEqualTo(3L); // Hades, nota 4.8
        // Sem perfil nao existe "porque voce gosta de...", entao a lista de generos
        // que justifica vem vazia e a tela mostra o texto de comunidade.
        assertThat(result.get(0).reasonGenres()).isEmpty();
    }

    @Test
    @DisplayName("respeita o limite de resultados")
    void respectsMaxResults() {
        ScoringWeights doisResultados = new ScoringWeights(3, 0.5, 1.5, 3.0, 2.0, 2);

        List<ScoredGame> result = engine.recommend(
                gostaDeRpg(), catalog(), List.of(), doisResultados);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("empate no score e desempatado pelo id, em ordem estavel")
    void tiesAreBrokenByGameIdForStableOrder() {
        // Dois jogos sem afinidade e com a MESMA nota media empatam no score. Sem
        // desempate explicito a ordem dependeria da ordem de chegada do catalogo, o
        // que faria a tela embaralhar entre requisicoes e este teste falhar de
        // forma intermitente. O catalogo abaixo vem com o id maior primeiro
        // justamente pra provar que o desempate age.
        List<CatalogGame> empatados = List.of(
                new CatalogGame(11L, "Jogo B", "url", "Sports", 4.0),
                new CatalogGame(10L, "Jogo A", "url", "Sports", 4.0));

        List<ScoredGame> result = engine.recommend(
                GameActivity.empty("novato"), empatados, List.of(), weights());

        assertThat(result).extracting(ScoredGame::gameId).containsExactly(10L, 11L);
    }

    @Test
    @DisplayName("catalogo vazio devolve lista vazia")
    void emptyCatalogYieldsNoRecommendations() {
        List<ScoredGame> result = engine.recommend(
                gostaDeRpg(), List.of(), List.of(), weights());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("informa os generos que justificam a recomendacao")
    void reportsTheGenresThatJustifyTheRecommendation() {
        // O microsservico devolve os GENEROS, nao uma frase pronta. Quem monta o
        // texto ("porque voce gosta de RPG") e o front - assim a apresentacao,
        // acentuacao e idioma ficam na camada que cuida disso, e a API continua
        // sendo dado estruturado.
        List<ScoredGame> result = engine.recommend(
                gostaDeRpg(), catalog(), List.of(), weights());

        ScoredGame eldenRing = result.stream()
                .filter(game -> game.gameId().equals(1L))
                .findFirst().orElseThrow();

        // Action nao entra: nao esta no perfil da ana.
        assertThat(eldenRing.reasonGenres()).containsExactly("RPG");
    }

    @Test
    @DisplayName("nao recomenda jogo sem nenhum candidato restante")
    void returnsEmptyWhenEveryGameIsExcluded() {
        // Usuario que ja avaliou tudo. Melhor lista vazia (a tela avisa) do que
        // repetir jogo que ele ja conhece.
        GameActivity avaliouTudo = new GameActivity("ana",
                catalog().stream()
                        .map(game -> new RatedGame(game.gameId(), game.genre(), 5))
                        .toList(),
                List.of());

        List<ScoredGame> result = engine.recommend(
                avaliouTudo, catalog(), List.of(), weights());

        assertThat(result).isEmpty();
    }
}
