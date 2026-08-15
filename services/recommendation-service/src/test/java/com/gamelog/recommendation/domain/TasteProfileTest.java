package com.gamelog.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// O perfil de gosto e a primeira metade do algoritmo: transforma "o que a pessoa
// avaliou e tem" em "quanto ela gosta de cada genero".
//
// A classe e pura - sem Spring, sem banco, sem HTTP - e por isso estes testes
// rodam em milissegundos e nao precisam de nenhum servico no ar. Foi de proposito:
// a regra de negocio do microsservico e justamente a parte que mais vale a pena
// cobrir, e nao daria pra cobrir bem se estivesse amarrada a infraestrutura.
class TasteProfileTest {

    // Os mesmos numeros de config-repo/recommendation-service.yml. Ficam
    // explicitos aqui pra cada teste ser lido sem consultar outro arquivo.
    private ScoringWeights weights() {
        return new ScoringWeights(3, 0.5, 1.5, 3.0, 2.0, 8);
    }

    @Test
    @DisplayName("separa os generos que vem juntos numa string")
    void splitsGenresPackedInASingleString() {
        // No catalogo o genero e uma string unica: "Action, RPG". O monolito guarda
        // assim desde o TP1 (e o formato que a API da RAWG devolve), entao o
        // microsservico precisa desempacotar antes de pontuar.
        GameActivity activity = new GameActivity("ana",
                List.of(new RatedGame(1L, "Action, RPG", 5)),
                List.of());

        TasteProfile profile = TasteProfile.from(activity, List.of(), List.of(), weights());

        assertThat(profile.weights()).containsOnlyKeys("Action", "RPG");
    }

    @Test
    @DisplayName("o genero favorito recebe peso 1.0 (normalizacao)")
    void favouriteGenreIsNormalisedToOne() {
        // Normalizar mantem a escala do score previsivel: sem isso, quem avaliou
        // 200 jogos teria pesos enormes e quem avaliou 3 teria pesos minusculos,
        // e o peso de comunidade nunca conseguiria competir.
        GameActivity activity = new GameActivity("ana",
                List.of(
                        new RatedGame(1L, "RPG", 5),
                        new RatedGame(2L, "RPG", 4),
                        new RatedGame(3L, "Sports", 3)),
                List.of());

        TasteProfile profile = TasteProfile.from(activity, List.of(), List.of(), weights());

        assertThat(profile.weights().get("RPG")).isCloseTo(1.0, offset(0.0001));
        assertThat(profile.weights().get("Sports")).isLessThan(1.0);
    }

    @Test
    @DisplayName("nota abaixo do minimo nao entra no perfil de gosto")
    void ratingBelowThresholdDoesNotShapeTaste() {
        // Nota 2 significa que a pessoa NAO gostou. Contar isso como afinidade
        // faria o servico recomendar mais do que ela rejeitou.
        GameActivity activity = new GameActivity("ana",
                List.of(new RatedGame(1L, "Sports", 2)),
                List.of());

        TasteProfile profile = TasteProfile.from(activity, List.of(), List.of(), weights());

        assertThat(profile.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("jogo so na colecao pesa menos que uma nota alta")
    void ownedGameCountsLessThanAHighRating() {
        // Ter o jogo diz menos sobre gosto do que ter gostado dele. O genero do
        // jogo possuido vem do catalogo, porque a colecao devolve so os ids.
        GameActivity activity = new GameActivity("ana",
                List.of(new RatedGame(1L, "RPG", 5)),
                List.of(2L));
        List<CatalogGame> catalog = List.of(
                new CatalogGame(2L, "FIFA", "url", "Sports", 3.0));

        TasteProfile profile = TasteProfile.from(activity, catalog, List.of(), weights());

        assertThat(profile.weights().get("Sports"))
                .isLessThan(profile.weights().get("RPG"));
    }

    @Test
    @DisplayName("feedback 'gostei' reforca o genero do jogo curtido")
    void likedFeedbackBoostsThatGenre() {
        // Este e o unico sinal que NASCE dentro do microsservico: o monolito nao
        // sabe que a pessoa curtiu uma recomendacao. E o dado exclusivo que
        // justifica o banco proprio.
        GameActivity activity = new GameActivity("ana",
                List.of(new RatedGame(1L, "RPG", 5)),
                List.of());
        List<CatalogGame> catalog = List.of(
                new CatalogGame(2L, "Hades", "url", "Roguelike", 4.5));

        TasteProfile semFeedback =
                TasteProfile.from(activity, catalog, List.of(), weights());
        TasteProfile comFeedback = TasteProfile.from(activity, catalog,
                List.of(new FeedbackEntry(2L, FeedbackVerdict.LIKED)), weights());

        assertThat(semFeedback.weights()).doesNotContainKey("Roguelike");
        assertThat(comFeedback.weights().get("Roguelike")).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("afinidade conta genero fora do perfil como zero")
    void affinityTreatsUnknownGenreAsZero() {
        // Um jogo "Action, Sports" pra quem so gosta de Action tem afinidade 0.5,
        // nao 1.0. Se generos desconhecidos fossem simplesmente ignorados na
        // media, jogos de genero unico dominariam o ranking - meio acerto passaria
        // a valer tanto quanto acerto cheio.
        GameActivity activity = new GameActivity("ana",
                List.of(new RatedGame(1L, "Action", 5)),
                List.of());

        TasteProfile profile = TasteProfile.from(activity, List.of(), List.of(), weights());

        assertThat(profile.affinityFor("Action")).isCloseTo(1.0, offset(0.0001));
        assertThat(profile.affinityFor("Action, Sports")).isCloseTo(0.5, offset(0.0001));
        assertThat(profile.affinityFor("Sports")).isCloseTo(0.0, offset(0.0001));
    }
}
