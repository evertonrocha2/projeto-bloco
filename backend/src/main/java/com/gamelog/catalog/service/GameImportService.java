package com.gamelog.catalog.service;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.dto.RawgDetail;
import com.gamelog.catalog.dto.RawgGame;
import com.gamelog.catalog.dto.RawgGenre;
import com.gamelog.catalog.dto.RawgListResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

// Conversa com a API externa de jogos (RAWG) e traduz a resposta dela pro nosso
// formato. Concentrar isso aqui deixa o resto do sistema sem saber que existe
// uma API de fora: pra ele, jogo e so a entidade Game.
@Service
public class GameImportService {

    private static final Logger log = LoggerFactory.getLogger(GameImportService.class);

    private final RestClient restClient;
    private final String apiKey;

    // A chave da RAWG e a URL base vem do application.properties.
    public GameImportService(
            @Value("${app.rawg.base-url}") String baseUrl,
            @Value("${app.rawg.key}") String apiKey
    ) {
        this.restClient = RestClient.create(baseUrl);
        this.apiKey = apiKey;
    }

    // Busca os jogos mais relevantes e devolve a quantidade pedida ja como Game.
    // A descricao NAO vem aqui (a lista da RAWG nao traz) - ela e buscada depois,
    // so quando alguem abre o jogo (ver fetchDescription).
    // Se a API falhar, devolve lista vazia em vez de derrubar a aplicacao.
    public List<Game> importPopularGames(int limit) {
        try {
            RawgListResponse response = restClient.get()
                    .uri(uri -> uri.path("/games")
                            .queryParam("key", apiKey)
                            .queryParam("ordering", "-added")
                            .queryParam("page_size", limit)
                            .build())
                    .retrieve()
                    .body(RawgListResponse.class);

            if (response == null || response.results() == null) {
                return List.of();
            }

            List<Game> games = response.results().stream()
                    // So aproveita jogos que tem imagem, pra nenhum card ficar quebrado.
                    .filter(g -> g.backgroundImage() != null && !g.backgroundImage().isBlank())
                    .map(this::toGame)
                    .toList();

            log.info("Importados {} jogos da RAWG", games.size());
            return games;
        } catch (Exception e) {
            log.warn("Nao foi possivel importar da RAWG: {}", e.getMessage());
            return List.of();
        }
    }

    // Busca a descricao de um jogo especifico no detalhe da RAWG. Usado sob
    // demanda. Devolve null se nao conseguir (e o catalogo segue funcionando).
    public String fetchDescription(Long externalId) {
        try {
            RawgDetail detail = restClient.get()
                    .uri(uri -> uri.path("/games/{id}")
                            .queryParam("key", apiKey)
                            .build(externalId))
                    .retrieve()
                    .body(RawgDetail.class);

            return detail == null ? null : detail.descriptionRaw();
        } catch (Exception e) {
            log.warn("Nao foi possivel buscar descricao do jogo {}: {}", externalId, e.getMessage());
            return null;
        }
    }

    // Traduz um item da RAWG na nossa entidade Game.
    private Game toGame(RawgGame source) {
        return new Game(
                source.id(),
                source.name(),
                null, // descricao preenchida depois
                parseYear(source.released()),
                joinGenres(source.genres()),
                source.backgroundImage()
        );
    }

    // Junta os generos numa string ("Action, RPG"). Esses sao os "topicos".
    private String joinGenres(List<RawgGenre> genres) {
        if (genres == null || genres.isEmpty()) {
            return "Sem gênero";
        }
        return genres.stream()
                .map(RawgGenre::name)
                .collect(Collectors.joining(", "));
    }

    // A data vem como "2013-09-17"; queremos so o ano.
    private Integer parseYear(String released) {
        if (released == null || released.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(released.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
