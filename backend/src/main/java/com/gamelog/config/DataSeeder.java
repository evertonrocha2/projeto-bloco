package com.gamelog.config;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.catalog.service.GameImportService;
import com.gamelog.collection.domain.CollectionEntry;
import com.gamelog.collection.repository.CollectionRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.review.repository.ReviewRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Popula o banco na PRIMEIRA vez que a aplicacao sobe: catalogo vindo de uma
// API externa + um usuario de demonstracao com reviews. Como agora o banco e
// persistido em arquivo, nas proximas execucoes ele ja vem com dados e o
// seeder nao faz nada (checagem de count logo abaixo) - e idempotente.
@Component
public class DataSeeder implements CommandLineRunner {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final CollectionRepository collectionRepository;
    private final GameImportService gameImportService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(GameRepository gameRepository,
                     UserRepository userRepository,
                     ReviewRepository reviewRepository,
                     CollectionRepository collectionRepository,
                     GameImportService gameImportService,
                     PasswordEncoder passwordEncoder) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.collectionRepository = collectionRepository;
        this.gameImportService = gameImportService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Se ja tem jogo no banco, nao faz nada (evita duplicar).
        if (gameRepository.count() > 0) {
            return;
        }

        // Primeiro tenta puxar da API externa. O filtro por externalId e uma
        // defesa extra contra duplicar jogo que ja existe no banco (aproveita
        // o indice idx_games_external_id).
        List<Game> imported = gameImportService.importPopularGames(24).stream()
                .filter(game -> game.getExternalId() == null
                        || gameRepository.findByExternalId(game.getExternalId()).isEmpty())
                .toList();

        // Se a API respondeu, usa o que veio dela; senao cai pra uma lista local
        // de seguranca pra app nunca abrir sem nenhum jogo.
        List<Game> games = imported.isEmpty()
                ? gameRepository.saveAll(fallbackGames())
                : gameRepository.saveAll(imported);

        // Usuario de demonstracao. Login: demo / demo123
        User demo = userRepository.save(new User(
                "demo",
                "demo@gamelog.com",
                passwordEncoder.encode("demo123"),
                "So um jogador que gosta de comentar sobre jogos."
        ));

        // Deixa o perfil e as paginas de jogo com algum conteudo de cara.
        // So cria se tiver jogos suficientes (a API pode variar a quantidade).
        if (games.size() >= 3) {
            reviewRepository.saveAll(List.of(
                    new Review(demo, games.get(0), 5,
                            "Joguei muito mais do que eu esperava. Recomendo demais."),
                    new Review(demo, games.get(1), 4,
                            "Bem divertido, mas precisa de um tempo pra pegar o jeito."),
                    new Review(demo, games.get(2), 3,
                            "Passa o tempo, mas nada que vai mudar sua vida.")
            ));

            // Tambem ja deixa alguns jogos na colecao do demo, com horas e status.
            collectionRepository.saveAll(List.of(
                    new CollectionEntry(demo, games.get(0), 120, "Zerado"),
                    new CollectionEntry(demo, games.get(1), 45, "Jogando"),
                    new CollectionEntry(demo, games.get(2), 0, "Quero jogar")
            ));
        }
    }

    // Lista de reserva, usada so se a API externa nao responder. O primeiro
    // parametro (externalId) vai null porque esses nao vieram da RAWG.
    private List<Game> fallbackGames() {
        return List.of(
                new Game(null, "The Legend of Zelda: Breath of the Wild",
                        "Um mundo aberto enorme onde voce explora do seu jeito.",
                        2017, "Aventura",
                        "https://upload.wikimedia.org/wikipedia/en/c/c6/The_Legend_of_Zelda_Breath_of_the_Wild.jpg"),
                new Game(null, "Elden Ring",
                        "RPG de acao da FromSoftware num mundo aberto desafiador.",
                        2022, "RPG de Acao",
                        "https://upload.wikimedia.org/wikipedia/en/b/b9/Elden_Ring_Box_art.jpg"),
                new Game(null, "Hades",
                        "Roguelike onde voce tenta escapar do submundo grego.",
                        2020, "Roguelike",
                        "https://upload.wikimedia.org/wikipedia/en/c/cc/Hades_cover_art.jpg")
        );
    }
}
