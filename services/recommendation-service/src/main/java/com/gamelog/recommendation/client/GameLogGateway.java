package com.gamelog.recommendation.client;

import com.gamelog.recommendation.client.dto.CatalogGamePayload;
import com.gamelog.recommendation.client.dto.GameActivityPayload;
import com.gamelog.recommendation.domain.CatalogGame;
import com.gamelog.recommendation.domain.GameActivity;
import com.gamelog.recommendation.domain.RatedGame;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// A ponte entre este servico e o monolito: chama, traduz e protege.
//
// === O disjuntor (circuit breaker) ===
//
// Sem ele, com o monolito fora do ar cada requisicao a este servico esperaria o
// timeout inteiro antes de falhar. Com varios usuarios na tela, as threads ficam
// todas presas esperando um servico que a gente JA SABE que nao esta respondendo -
// e a falha de um servico derruba o outro. E o efeito cascata.
//
// O disjuntor conta as falhas recentes e, passando do limite, ABRE: as chamadas
// seguintes vao direto pro fallback, sem tentar a rede. Depois de um tempo ele
// deixa passar algumas pra testar se o outro lado voltou.
//
// O plano B aqui e util de verdade, e nao uma mensagem de erro: o ultimo lote de
// recomendacoes esta no banco proprio deste servico, entao a tela continua
// mostrando conteudo - marcado como desatualizado. E o que justifica, na pratica,
// o microsservico ter banco proprio.
@Component
public class GameLogGateway implements ActivitySource {

    private static final Logger log = LoggerFactory.getLogger(GameLogGateway.class);

    private final GameLogClient client;

    public GameLogGateway(GameLogClient client) {
        this.client = client;
    }

    // O 404 de "usuario sem atividade" e tratado dentro de fetchActivity e nunca
    // chega aqui - de proposito. Se chegasse, o disjuntor contaria como falha, e
    // consultar usuarios novos abriria o circuito e tiraria a feature do ar pra
    // todo mundo. Distinguir "o outro servico esta com problema" de "a resposta
    // dele foi negativa" e o que faz um disjuntor ser util em vez de atrapalhar.
    @Override
    @CircuitBreaker(name = "gamelog", fallbackMethod = "gameLogUnavailable")
    public Optional<GameLogSnapshot> fetch(String username) {
        GameActivity activity = fetchActivity(username);
        List<CatalogGame> catalog = client.listGames().stream()
                .map(this::toCatalogGame)
                .toList();

        return Optional.of(new GameLogSnapshot(activity, catalog));
    }

    // Metodo de fallback. A assinatura precisa ser a mesma de fetch(), mais o
    // Throwable no fim - e assim que o Resilience4j o encontra.
    //
    // Devolve Optional.empty(), e nao lanca: quem chama trata "sem retrato" como
    // cenario previsto e serve o lote gravado. Ver ActivitySource.
    private Optional<GameLogSnapshot> gameLogUnavailable(String username, Throwable throwable) {
        log.warn("Monolito GameLog indisponivel ao buscar dados de '{}': {}. "
                        + "Servindo as recomendacoes gravadas anteriormente.",
                username, throwable.toString());
        return Optional.empty();
    }

    // 404 aqui significa "usuario sem atividade", nao erro: devolve atividade
    // vazia. O catalogo continua sendo buscado, entao a pessoa recebe os jogos mais
    // bem avaliados da comunidade em vez de uma tela em branco.
    private GameActivity fetchActivity(String username) {
        try {
            GameActivityPayload payload = client.getGameActivity(username);
            return toActivity(username, payload);
        } catch (FeignException.NotFound notFound) {
            log.info("Usuario '{}' sem atividade no GameLog; recomendando pela nota da comunidade.",
                    username);
            return GameActivity.empty(username);
        }
    }

    private GameActivity toActivity(String username, GameActivityPayload payload) {
        if (payload == null) {
            return GameActivity.empty(username);
        }

        List<RatedGame> rated = payload.ratedGames() == null
                ? List.of()
                : payload.ratedGames().stream()
                        .map(item -> new RatedGame(item.gameId(), item.genre(), item.rating()))
                        .toList();

        List<Long> owned = payload.ownedGameIds() == null ? List.of() : payload.ownedGameIds();

        return new GameActivity(username, rated, owned);
    }

    // Atencao: o catalogo do monolito chama o identificador de "id".
    private CatalogGame toCatalogGame(CatalogGamePayload payload) {
        return new CatalogGame(
                payload.id(),
                payload.title(),
                payload.coverUrl(),
                payload.genre(),
                payload.averageRating());
    }
}
