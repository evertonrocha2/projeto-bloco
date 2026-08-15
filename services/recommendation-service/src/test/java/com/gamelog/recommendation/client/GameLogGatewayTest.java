package com.gamelog.recommendation.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.recommendation.client.dto.CatalogGamePayload;
import com.gamelog.recommendation.client.dto.GameActivityPayload;
import com.gamelog.recommendation.client.dto.RatedGamePayload;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Testa a TRADUCAO entre o JSON do monolito e o dominio deste servico.
//
// E um ponto de falha silenciosa: os nomes dos campos nao sao verificados em
// tempo de compilacao, e o monolito chama o identificador do jogo de "id" no
// catalogo e de "gameId" na atividade. Mapear errado nao quebra nada
// visivelmente - so produz recomendacoes vazias ou sem sentido.
//
// Cobertura desta classe, e o que fica de fora de proposito:
//  - AQUI: mapeamento dos payloads e tratamento de usuario inexistente (404).
//  - No RecommendationServiceTest: o comportamento "monolito fora do ar serve o
//    lote anterior", atraves de um ActivitySource falso.
//  - O disjuntor em si (@CircuitBreaker) depende do proxy AOP do Spring, entao
//    nao age num teste unitario. Ele e verificado derrubando o monolito com a
//    stack no ar - o roteiro esta em docs/MICROSSERVICO.md. Engolir a excecao
//    aqui pra facilitar o teste seria pior: o disjuntor nunca veria a falha e
//    nunca abriria.
class GameLogGatewayTest {

    // Duplo do cliente Feign. Escrito a mao: precisa apenas devolver payloads ou
    // estourar 404.
    private static class FakeGameLogClient implements GameLogClient {
        private final boolean userExists;

        FakeGameLogClient(boolean userExists) {
            this.userExists = userExists;
        }

        @Override
        public GameActivityPayload getGameActivity(String username) {
            if (!userExists) {
                throw notFound();
            }
            return new GameActivityPayload(username,
                    List.of(new RatedGamePayload(99L, "RPG", 5)),
                    List.of(7L));
        }

        @Override
        public List<CatalogGamePayload> listGames() {
            return List.of(new CatalogGamePayload(
                    1L, "Elden Ring", "Action, RPG", "url1", 4.5));
        }

        private FeignException.NotFound notFound() {
            return new FeignException.NotFound("nao encontrado",
                    Request.create(Request.HttpMethod.GET, "/api/users/x/game-activity",
                            java.util.Map.of(), null, new RequestTemplate()),
                    null, null);
        }
    }

    @Test
    @DisplayName("traduz o payload do monolito para o dominio")
    void translatesMonolithPayloadIntoDomain() {
        GameLogGateway gateway = new GameLogGateway(new FakeGameLogClient(true));

        Optional<GameLogSnapshot> snapshot = gateway.fetch("ana");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().activity().ratedGames())
                .singleElement()
                .satisfies(rated -> {
                    assertThat(rated.genre()).isEqualTo("RPG");
                    assertThat(rated.rating()).isEqualTo(5);
                });
        assertThat(snapshot.get().activity().ownedGameIds()).containsExactly(7L);
    }

    @Test
    @DisplayName("le o id do catalogo do campo 'id', nao 'gameId'")
    void readsCatalogIdFromTheIdField() {
        // O GameResponse do monolito chama o identificador de "id". Se o mapeamento
        // pegasse o campo errado, todo gameId viria nulo e nenhuma recomendacao
        // conseguiria ser gravada - sem nenhum erro de compilacao avisando.
        GameLogGateway gateway = new GameLogGateway(new FakeGameLogClient(true));

        Optional<GameLogSnapshot> snapshot = gateway.fetch("ana");

        assertThat(snapshot.get().catalog())
                .singleElement()
                .satisfies(game -> {
                    assertThat(game.gameId()).isEqualTo(1L);
                    assertThat(game.title()).isEqualTo("Elden Ring");
                    assertThat(game.averageRating()).isEqualTo(4.5);
                });
    }

    @Test
    @DisplayName("usuario sem cadastro no monolito continua recebendo o catalogo")
    void unknownUserStillGetsTheCatalogue() {
        // 404 do monolito NAO e falha de infraestrutura: e resposta valida dizendo
        // "esse usuario nao tem atividade". Tratar como falha abriria o disjuntor a
        // cada consulta de usuario novo e derrubaria a feature pra todo mundo.
        // Devolvendo o catalogo com atividade vazia, o usuario recebe os jogos mais
        // bem avaliados da comunidade.
        GameLogGateway gateway = new GameLogGateway(new FakeGameLogClient(false));

        Optional<GameLogSnapshot> snapshot = gateway.fetch("fantasma");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().activity().ratedGames()).isEmpty();
        assertThat(snapshot.get().catalog()).isNotEmpty();
    }
}
