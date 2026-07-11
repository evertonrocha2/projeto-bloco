package com.gamelog.catalog.service;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.shared.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Service do catalogo: listar jogos, buscar um pelo id e busca paginada.
// Quando alguem abre um jogo pela primeira vez e ele ainda nao tem descricao,
// a gente busca na API externa naquele momento e salva - assim o startup fica
// rapido (so a lista) e a descricao chega quando realmente precisa.
@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GameImportService gameImportService;

    public GameService(GameRepository gameRepository, GameImportService gameImportService) {
        this.gameRepository = gameRepository;
        this.gameImportService = gameImportService;
    }

    @Transactional(readOnly = true)
    public List<Game> findAll() {
        return gameRepository.findAll();
    }

    // Busca paginada por titulo. Com titulo vazio devolve o catalogo inteiro,
    // so que em paginas - o banco aplica LIMIT/OFFSET em vez de trazer tudo.
    @Transactional(readOnly = true)
    public Page<Game> search(String title, Pageable pageable) {
        String term = title == null ? "" : title.trim();
        return gameRepository.findByTitleContainingIgnoreCase(term, pageable);
    }

    @Transactional
    public Game findById(Long id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Jogo nao encontrado"));

        // Lazy load da descricao: so busca se ainda nao tem e se o jogo veio da
        // API externa (tem externalId). Depois salva pra nao buscar de novo.
        boolean semDescricao = game.getDescription() == null || game.getDescription().isBlank();
        if (semDescricao && game.getExternalId() != null) {
            String description = gameImportService.fetchDescription(game.getExternalId());
            if (description != null && !description.isBlank()) {
                game.setDescription(description);
                gameRepository.save(game);
            }
        }
        return game;
    }
}
