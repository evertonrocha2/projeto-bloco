package com.gamelog.recommendation.activity;

import com.gamelog.recommendation.domain.CatalogGame;
import com.gamelog.recommendation.domain.GameActivity;
import com.gamelog.recommendation.domain.RatedGame;
import com.gamelog.recommendation.projection.CatalogGameView;
import com.gamelog.recommendation.projection.CatalogGameViewRepository;
import com.gamelog.recommendation.projection.ProjectionCheckpoint;
import com.gamelog.recommendation.projection.ProjectionCheckpointRepository;
import com.gamelog.recommendation.projection.UserCollectionView;
import com.gamelog.recommendation.projection.UserRatingViewRepository;
import com.gamelog.recommendation.projection.UserCollectionViewRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// O retrato do GameLog montado a partir da projecao local - sem rede.
//
// === O que mudou em relacao ao TP3 ===
//
// Antes: cada recalculo fazia duas chamadas HTTP ao monolito (atividade + catalogo
// inteiro), atravessando Eureka, LoadBalancer e circuit breaker. Com o monolito
// fora, a feature entrava em modo degradado.
//
// Agora: tres consultas ao banco PROPRIO. O monolito pode estar fora do ar, lento
// ou sendo reimplantado - o recalculo nao fica sabendo. O que o usuario perde
// nesse cenario e so frescor: eventos que o monolito ainda nao publicou.
//
// O preco dessa independencia e a CONSISTENCIA EVENTUAL. Entre a review ser salva
// no monolito e o evento ser aplicado aqui passam alguns centenas de
// milissegundos, e nesse intervalo as duas visoes divergem. Pra uma recomendacao
// de jogo, e um preco otimo.
@Component
public class ProjectionActivitySource implements ActivitySource {

    private final CatalogGameViewRepository catalogRepository;
    private final UserRatingViewRepository ratingRepository;
    private final UserCollectionViewRepository collectionRepository;
    private final ProjectionCheckpointRepository checkpointRepository;

    public ProjectionActivitySource(CatalogGameViewRepository catalogRepository,
                                    UserRatingViewRepository ratingRepository,
                                    UserCollectionViewRepository collectionRepository,
                                    ProjectionCheckpointRepository checkpointRepository) {
        this.catalogRepository = catalogRepository;
        this.ratingRepository = ratingRepository;
        this.collectionRepository = collectionRepository;
        this.checkpointRepository = checkpointRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameLogSnapshot> fetch(String username) {
        List<CatalogGameView> games = catalogRepository.findAll();

        // Sem snapshot aplicado E sem catalogo, a projecao nao sabe nada ainda: o
        // servico acabou de subir pela primeira vez e o monolito nao respondeu o
        // pedido de snapshot. E o unico caso de "nao ha retrato".
        if (games.isEmpty() && !checkpointRepository.existsById(ProjectionCheckpoint.SNAPSHOT)) {
            return Optional.empty();
        }

        Map<Long, Double> averageByGame = new HashMap<>();
        for (Object[] row : ratingRepository.averageRatingByGame()) {
            averageByGame.put((Long) row[0], ((Number) row[1]).doubleValue());
        }

        Map<Long, String> genreByGame = new HashMap<>();
        List<CatalogGame> catalog = games.stream()
                .peek(game -> genreByGame.put(game.getGameId(), game.getGenre()))
                .map(game -> new CatalogGame(
                        game.getGameId(),
                        game.getTitle(),
                        game.getCoverUrl(),
                        game.getGenre(),
                        averageByGame.getOrDefault(game.getGameId(), 0.0)))
                .toList();

        List<RatedGame> rated = ratingRepository.findByUsernameAndDeletedFalse(username).stream()
                .map(rating -> new RatedGame(rating.getGameId(), genreByGame.get(rating.getGameId()), rating.getRating()))
                .toList();

        List<Long> owned = collectionRepository.findByUsername(username).stream()
                .map(UserCollectionView::getGameId)
                .toList();

        return Optional.of(new GameLogSnapshot(new GameActivity(username, rated, owned), catalog));
    }
}
