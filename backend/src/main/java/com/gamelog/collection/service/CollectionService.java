package com.gamelog.collection.service;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.collection.domain.CollectionEntry;
import com.gamelog.collection.dto.AddToCollectionRequest;
import com.gamelog.collection.dto.CollectionEntryResponse;
import com.gamelog.collection.dto.CollectionRevisionResponse;
import com.gamelog.collection.repository.CollectionRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.shared.BadRequestException;
import com.gamelog.shared.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Regras da colecao: adicionar/atualizar um jogo na colecao de quem esta logado,
// listar a colecao de um usuario e consultar o historico de um item. Se o jogo
// ja estiver na colecao, a gente atualiza as horas e o status em vez de criar
// duplicado - e cada atualizacao vira uma revisao no historico (Envers).
@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public CollectionService(CollectionRepository collectionRepository,
                            UserRepository userRepository,
                            GameRepository gameRepository) {
        this.collectionRepository = collectionRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional
    public CollectionEntryResponse addOrUpdate(String username, AddToCollectionRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));

        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new NotFoundException("Jogo nao encontrado"));

        // Ja esta na colecao? Atualiza. Senao, cria.
        CollectionEntry entry = collectionRepository
                .findByUserIdAndGameId(user.getId(), game.getId())
                .map(existing -> {
                    existing.setHoursPlayed(request.hoursPlayed());
                    existing.setStatus(request.status());
                    return existing;
                })
                .orElseGet(() -> new CollectionEntry(user, game, request.hoursPlayed(), request.status()));

        collectionRepository.save(entry);
        return CollectionEntryResponse.from(entry);
    }

    @Transactional(readOnly = true)
    public List<CollectionEntryResponse> findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));

        return collectionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(CollectionEntryResponse::from)
                .toList();
    }

    // Linha do tempo de um item da colecao: toda mudanca de horas/status que ja
    // aconteceu, com data e autor. So o dono do item pode consultar.
    @Transactional(readOnly = true)
    public List<CollectionRevisionResponse> history(String username, Long entryId) {
        CollectionEntry entry = collectionRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Item da colecao nao encontrado"));

        if (!entry.getUser().getUsername().equals(username)) {
            throw new BadRequestException("Esse item da colecao nao e seu");
        }

        return collectionRepository.findRevisions(entryId).stream()
                .map(CollectionRevisionResponse::from)
                .toList();
    }
}
