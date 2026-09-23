package com.gamelog.recommendation.projection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.recommendation.messaging.EventPayloads.CollectionEvent;
import com.gamelog.recommendation.messaging.EventPayloads.GameEvent;
import com.gamelog.recommendation.messaging.EventPayloads.ReviewEvent;
import com.gamelog.recommendation.messaging.GameLogEvent;
import com.gamelog.recommendation.messaging.SnapshotMessages.CatalogSnapshot;
import com.gamelog.recommendation.messaging.SnapshotMessages.Ownership;
import com.gamelog.recommendation.messaging.SnapshotMessages.Rating;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Aplica eventos e snapshots na copia local dos dados do GameLog.
//
// Tres regras sustentam a projecao, e as tres existem porque a entrega pelo broker
// e "pelo menos uma vez" e nao garante que o mundo seja bem comportado:
//
//  1. Idempotencia: cada eventId e aplicado uma vez so (ProcessedEvent).
//  2. Ultimo fato vence: um evento mais velho que o estado gravado e ignorado
//     (lastEventAt). Protege contra reentrega atrasada.
//  3. Lapide no delete: nota apagada vira deleted=true, e nao some - senao um
//     evento atrasado a ressuscitaria.
//
// Tudo numa transacao so: o registro de "processado" e a mudanca na projecao
// entram juntos ou nao entram. Se o metodo estourar, o broker reentrega a
// mensagem, e ela encontra o banco exatamente como estava antes.
@Service
public class ActivityProjector {

    private static final Logger log = LoggerFactory.getLogger(ActivityProjector.class);

    // Versao do payload que este servico sabe ler.
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final CatalogGameViewRepository catalogRepository;
    private final UserRatingViewRepository ratingRepository;
    private final UserCollectionViewRepository collectionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ProjectionCheckpointRepository checkpointRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ActivityProjector(CatalogGameViewRepository catalogRepository,
                             UserRatingViewRepository ratingRepository,
                             UserCollectionViewRepository collectionRepository,
                             ProcessedEventRepository processedEventRepository,
                             ProjectionCheckpointRepository checkpointRepository,
                             ObjectMapper objectMapper,
                             Clock clock) {
        this.catalogRepository = catalogRepository;
        this.ratingRepository = ratingRepository;
        this.collectionRepository = collectionRepository;
        this.processedEventRepository = processedEventRepository;
        this.checkpointRepository = checkpointRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public ProjectionOutcome apply(GameLogEvent event) {
        if (event.schemaVersion() > SUPPORTED_SCHEMA_VERSION) {
            // Formato novo que este servico ainda nao entende. Aplicar "no escuro"
            // corromperia a projecao; recusar manda a mensagem pra DLQ, onde ela
            // espera ate este servico ser atualizado e ela ser reprocessada.
            throw new UnsupportedEventVersionException(event);
        }

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Evento {} ({}) ja aplicado; ignorando a reentrega", event.eventId(), event.eventType());
            return ProjectionOutcome.duplicateEvent();
        }

        ProjectionOutcome outcome = switch (event.eventType()) {
            case "review.created", "review.updated" -> upsertRating(read(event, ReviewEvent.class), event.occurredAt());
            case "review.deleted" -> deleteRating(read(event, ReviewEvent.class), event.occurredAt());
            case "collection.updated" -> upsertCollection(read(event, CollectionEvent.class), event.occurredAt());
            case "catalog.game.added" -> upsertGame(read(event, GameEvent.class), event.occurredAt());
            default -> {
                // A fila so recebe o que foi assinado, mas um binding novo por engano
                // nao deve derrubar o consumidor. Evento desconhecido e anotado como
                // processado e segue a vida.
                log.debug("Evento {} de tipo {} nao interessa a esta projecao", event.eventId(), event.eventType());
                yield ProjectionOutcome.nothing();
            }
        };

        processedEventRepository.save(new ProcessedEvent(event.eventId(), event.eventType(), clock.instant()));
        return outcome;
    }

    // Aplica o snapshot inicial. Vale a mesma regra do "ultimo fato vence": uma
    // linha que um evento ja atualizou DEPOIS do instante do snapshot fica como
    // esta, porque e mais nova que a foto.
    @Transactional
    public void applySnapshot(CatalogSnapshot snapshot) {
        Instant takenAt = snapshot.generatedAt();

        snapshot.games().forEach(game -> upsertGame(game, takenAt));

        Map<String, Rating> ratingsInSnapshot = new HashMap<>();
        snapshot.ratings().forEach(rating -> ratingsInSnapshot.put(key(rating.username(), rating.gameId()), rating));

        // Reconcilia o que ja existe: nota que nao esta no snapshot (e e mais velha
        // que ele) foi apagada no monolito enquanto este servico nao ouvia.
        for (UserRatingView existing : ratingRepository.findAll()) {
            if (existing.isNewerThan(takenAt)) {
                ratingsInSnapshot.remove(key(existing.getUsername(), existing.getGameId()));
                continue;
            }
            Rating fromSnapshot = ratingsInSnapshot.remove(key(existing.getUsername(), existing.getGameId()));
            if (fromSnapshot == null) {
                existing.markDeleted(takenAt);
            } else {
                existing.rate(fromSnapshot.rating(), takenAt);
            }
        }
        ratingsInSnapshot.values().forEach(rating -> ratingRepository.save(
                new UserRatingView(rating.username(), rating.gameId(), rating.rating(), takenAt)));

        Map<String, Ownership> ownedInSnapshot = new HashMap<>();
        snapshot.collection().forEach(owned -> ownedInSnapshot.put(key(owned.username(), owned.gameId()), owned));
        for (UserCollectionView existing : collectionRepository.findAll()) {
            Ownership fromSnapshot = ownedInSnapshot.remove(key(existing.getUsername(), existing.getGameId()));
            if (!existing.isNewerThan(takenAt) && fromSnapshot != null) {
                existing.update(fromSnapshot.status(), takenAt);
            }
        }
        ownedInSnapshot.values().forEach(owned -> collectionRepository.save(
                new UserCollectionView(owned.username(), owned.gameId(), owned.status(), takenAt)));

        checkpointRepository.save(new ProjectionCheckpoint(ProjectionCheckpoint.SNAPSHOT, takenAt));
        log.info("Snapshot de {} aplicado: {} jogos, {} notas, {} itens de colecao",
                takenAt, snapshot.games().size(), snapshot.ratings().size(), snapshot.collection().size());
    }

    private ProjectionOutcome upsertRating(ReviewEvent review, Instant occurredAt) {
        rememberGame(review.gameId(), review.gameTitle(), review.genre(), occurredAt);

        var existing = ratingRepository.findByUsernameAndGameId(review.username(), review.gameId());
        if (existing.isPresent()) {
            if (existing.get().isNewerThan(occurredAt)) {
                log.info("Nota de {} no jogo {} ja e mais nova que o evento; ignorando", review.username(), review.gameId());
                return ProjectionOutcome.nothing();
            }
            existing.get().rate(review.rating(), occurredAt);
        } else {
            ratingRepository.save(new UserRatingView(review.username(), review.gameId(), review.rating(), occurredAt));
        }
        return ProjectionOutcome.forUser(review.username());
    }

    private ProjectionOutcome deleteRating(ReviewEvent review, Instant occurredAt) {
        var existing = ratingRepository.findByUsernameAndGameId(review.username(), review.gameId());
        if (existing.isPresent()) {
            if (existing.get().isNewerThan(occurredAt)) {
                return ProjectionOutcome.nothing();
            }
            existing.get().markDeleted(occurredAt);
        } else {
            // O delete chegou antes da criacao (ou a criacao se perdeu antes do
            // snapshot). A lapide ja nasce, pra barrar a criacao atrasada.
            UserRatingView tombstone = new UserRatingView(review.username(), review.gameId(), review.rating(), occurredAt);
            tombstone.markDeleted(occurredAt);
            ratingRepository.save(tombstone);
        }
        return ProjectionOutcome.forUser(review.username());
    }

    private ProjectionOutcome upsertCollection(CollectionEvent entry, Instant occurredAt) {
        rememberGame(entry.gameId(), entry.gameTitle(), entry.genre(), occurredAt);

        var existing = collectionRepository.findByUsernameAndGameId(entry.username(), entry.gameId());
        if (existing.isPresent()) {
            if (existing.get().isNewerThan(occurredAt)) {
                return ProjectionOutcome.nothing();
            }
            existing.get().update(entry.status(), occurredAt);
        } else {
            collectionRepository.save(new UserCollectionView(entry.username(), entry.gameId(), entry.status(), occurredAt));
        }
        return ProjectionOutcome.forUser(entry.username());
    }

    private ProjectionOutcome upsertGame(GameEvent game, Instant occurredAt) {
        boolean isNew = !catalogRepository.existsById(game.gameId());
        catalogRepository.findById(game.gameId()).ifPresentOrElse(
                known -> known.refresh(game.title(), game.genre(), game.coverUrl(), occurredAt),
                () -> catalogRepository.save(
                        new CatalogGameView(game.gameId(), game.title(), game.genre(), game.coverUrl(), occurredAt)));
        return isNew ? ProjectionOutcome.catalog() : ProjectionOutcome.nothing();
    }

    // Um evento de review pode citar um jogo que o catalogo local ainda nao tem
    // (o catalog.game.added dele veio antes de este servico existir). Os dados do
    // proprio evento bastam pra criar a entrada; a capa chega no snapshot.
    private void rememberGame(Long gameId, String title, String genre, Instant occurredAt) {
        if (gameId != null && title != null && !catalogRepository.existsById(gameId)) {
            catalogRepository.save(new CatalogGameView(gameId, title, genre, null, occurredAt));
        }
    }

    private <T> T read(GameLogEvent event, Class<T> type) {
        try {
            T payload = event.payload() == null ? null : objectMapper.treeToValue(event.payload(), type);
            if (payload == null) {
                throw new InvalidEventException("Evento " + event.eventId() + " sem payload", null);
            }
            return payload;
        } catch (JsonProcessingException e) {
            throw new InvalidEventException("Payload invalido no evento " + event.eventId(), e);
        }
    }

    private static String key(String username, Long gameId) {
        return username + "#" + gameId;
    }
}
