package com.gamelog.integration.snapshot;

import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.collection.domain.CollectionStatus;
import com.gamelog.collection.repository.CollectionRepository;
import com.gamelog.messaging.event.GameEventPayload;
import com.gamelog.review.repository.ReviewRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Monta o snapshot a partir do banco do monolito.
//
// As tres leituras acontecem na MESMA transacao de leitura: o snapshot tem que ser
// uma foto de um instante so. Lidas em transacoes separadas, uma review criada
// entre a primeira e a segunda consulta poderia aparecer nas notas sem o jogo dela
// estar no catalogo.
@Service
public class SnapshotService {

    private final GameRepository gameRepository;
    private final ReviewRepository reviewRepository;
    private final CollectionRepository collectionRepository;
    private final Clock clock;

    public SnapshotService(GameRepository gameRepository,
                           ReviewRepository reviewRepository,
                           CollectionRepository collectionRepository,
                           Clock clock) {
        this.gameRepository = gameRepository;
        this.reviewRepository = reviewRepository;
        this.collectionRepository = collectionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CatalogSnapshot current() {
        // O instante e tirado ANTES das leituras. Um evento gravado durante a
        // montagem tem occurredAt maior que este, entao o consumidor ainda o aplica
        // por cima do snapshot - nada se perde na costura entre os dois.
        var generatedAt = clock.instant();

        List<GameEventPayload> games = gameRepository.findAll().stream()
                .map(GameEventPayload::from)
                .toList();

        List<OwnershipRow> collection = collectionRepository.findAllOwnershipRows().stream()
                .map(row -> new OwnershipRow(
                        (String) row[0], (Long) row[1], ((CollectionStatus) row[2]).name()))
                .toList();

        return new CatalogSnapshot(generatedAt, games, reviewRepository.findAllRatingRows(), collection);
    }
}
