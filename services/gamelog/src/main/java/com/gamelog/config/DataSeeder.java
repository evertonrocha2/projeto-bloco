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
    //
    // Ate o TP2 esta lista tinha tres jogos - o bastante pra aplicacao nao abrir
    // vazia. Com o microsservico de recomendacoes do TP3 isso deixou de bastar:
    // o usuario de demonstracao avalia os tres primeiros jogos e adiciona os
    // mesmos tres a colecao, entao NAO SOBRAVA CANDIDATO NENHUM. O algoritmo
    // respondia corretamente com lista vazia, e quem subisse o projeto sem chave
    // da RAWG concluiria que a feature nao funciona.
    //
    // Os generos foram escolhidos pra que a recomendacao seja observavel: alguns
    // jogos compartilham genero com os que o demo avaliou bem (e devem subir no
    // ranking), outros nao (e devem ficar embaixo). Da pra conferir o algoritmo
    // trabalhando sem precisar de chave de API.
    //
    // As capas vem da CDN publica da Steam (library_600x900 = poster retrato, o
    // formato que a interface usa). A primeira versao desta lista apontava pra
    // Wikipedia e CINCO das doze davam 404 - caminhos montados na mao, sem
    // conferir. A CDN da Steam e mais adequada por tres motivos: e enderecada por
    // appid (nao por nome de arquivo adivinhado), e servida pra hotlink, e o
    // formato e sempre o mesmo. Cada URL abaixo foi verificada respondendo 200.
    //
    // A excecao e Zelda, que e exclusivo da Nintendo e nao existe na Steam.
    private List<Game> fallbackGames() {
        return List.of(
                // --- os tres primeiros sao os que o usuario demo avalia ---
                new Game(null, "The Legend of Zelda: Breath of the Wild",
                        "Um mundo aberto enorme onde voce explora do seu jeito.",
                        2017, "Aventura",
                        "https://upload.wikimedia.org/wikipedia/en/c/c6/The_Legend_of_Zelda_Breath_of_the_Wild.jpg"),
                new Game(null, "Elden Ring",
                        "RPG de acao da FromSoftware num mundo aberto desafiador.",
                        2022, "RPG de Acao",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/1245620/library_600x900.jpg"),
                new Game(null, "Hades",
                        "Roguelike onde voce tenta escapar do submundo grego.",
                        2020, "Roguelike",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/1145360/library_600x900.jpg"),

                // --- daqui pra baixo, os candidatos a recomendacao ---
                // Compartilham "Aventura" e "RPG de Acao" com o que o demo curtiu,
                // entao aparecem no topo das recomendacoes dele.
                new Game(null, "God of War",
                        "Kratos e Atreus atravessam a mitologia nordica.",
                        2018, "RPG de Acao, Aventura",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/1593500/library_600x900.jpg"),
                new Game(null, "The Witcher 3: Wild Hunt",
                        "Geralt procura Ciri num mundo aberto cheio de escolhas.",
                        2015, "RPG, Aventura",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/292030/library_600x900.jpg"),
                new Game(null, "Hollow Knight",
                        "Metroidvania desenhado a mao num reino de insetos.",
                        2017, "Metroidvania, Aventura",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/367520/library_600x900.jpg"),
                new Game(null, "Sekiro: Shadows Die Twice",
                        "Acao com espadas e parry no Japao do periodo Sengoku.",
                        2019, "RPG de Acao",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/814380/library_600x900.jpg"),

                // Genero proximo do que ele avaliou pior (Roguelike, nota 3):
                // aparecem, mas mais embaixo.
                new Game(null, "Dead Cells",
                        "Roguelike de acao rapida onde morrer e parte do plano.",
                        2018, "Roguelike, Metroidvania",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/588650/library_600x900.jpg"),
                new Game(null, "Slay the Spire",
                        "Baralho de cartas que sobe uma torre, uma escolha por vez.",
                        2019, "Roguelike, Cartas",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/646570/library_600x900.jpg"),

                // Sem afinidade com o perfil do demo: servem de contraste no ranking.
                new Game(null, "Celeste",
                        "Plataforma dificil sobre subir uma montanha e a si mesmo.",
                        2018, "Plataforma, Indie",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/504230/library_600x900.jpg"),
                new Game(null, "Stardew Valley",
                        "Voce herda uma fazenda e recomeca a vida no campo.",
                        2016, "Simulacao, Indie",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/413150/library_600x900.jpg"),
                new Game(null, "Disco Elysium",
                        "RPG de detetive onde a conversa e o combate.",
                        2019, "RPG",
                        "https://cdn.cloudflare.steamstatic.com/steam/apps/632470/library_600x900.jpg")
        );
    }
}
