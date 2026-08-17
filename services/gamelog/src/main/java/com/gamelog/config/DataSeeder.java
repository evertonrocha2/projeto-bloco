package com.gamelog.config;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.catalog.service.GameImportService;
import com.gamelog.collection.domain.CollectionEntry;
import com.gamelog.collection.domain.CollectionStatus;
import com.gamelog.collection.repository.CollectionRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.list.domain.GameList;
import com.gamelog.list.domain.ListVisibility;
import com.gamelog.list.repository.GameListRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.review.domain.ReviewReply;
import com.gamelog.review.domain.ReviewVote;
import com.gamelog.review.domain.VoteType;
import com.gamelog.review.repository.ReviewReplyRepository;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.review.repository.ReviewVoteRepository;
import java.util.List;
import java.util.Set;
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
    private final ReviewVoteRepository reviewVoteRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final GameListRepository gameListRepository;
    private final GameImportService gameImportService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(GameRepository gameRepository,
                     UserRepository userRepository,
                     ReviewRepository reviewRepository,
                     CollectionRepository collectionRepository,
                     ReviewVoteRepository reviewVoteRepository,
                     ReviewReplyRepository reviewReplyRepository,
                     GameListRepository gameListRepository,
                     GameImportService gameImportService,
                     PasswordEncoder passwordEncoder) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.collectionRepository = collectionRepository;
        this.reviewVoteRepository = reviewVoteRepository;
        this.reviewReplyRepository = reviewReplyRepository;
        this.gameListRepository = gameListRepository;
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

        // Segunda pessoa. Existe pra camada social ter com quem conversar: voto e
        // resposta sao proibidos na propria avaliacao, entao com um usuario so
        // NENHUMA das duas features seria observavel ao subir o projeto.
        User critica = userRepository.save(new User(
                "critica",
                "critica@gamelog.com",
                passwordEncoder.encode("demo123"),
                "Jogo pouco e falo muito."
        ));

        // Deixa o perfil e as paginas de jogo com algum conteudo de cara.
        // So cria se tiver jogos suficientes (a API pode variar a quantidade).
        if (games.size() >= 3) {
            Review primeira = reviewRepository.save(new Review(demo, games.get(0), 5,
                    "Joguei muito mais do que eu esperava. Recomendo demais."));
            reviewRepository.saveAll(List.of(
                    new Review(demo, games.get(1), 4,
                            "Bem divertido, mas precisa de um tempo pra pegar o jeito."),
                    new Review(demo, games.get(2), 3,
                            "Passa o tempo, mas nada que vai mudar sua vida."),
                    new Review(critica, games.get(0), 3,
                            "Bonito, mas repetitivo depois da terceira regiao.")
            ));

            // Tambem ja deixa alguns jogos na colecao do demo, com horas e status.
            collectionRepository.saveAll(List.of(
                    new CollectionEntry(demo, games.get(0), 120, CollectionStatus.PLATINADO),
                    new CollectionEntry(demo, games.get(1), 45, CollectionStatus.JOGANDO),
                    new CollectionEntry(demo, games.get(2), 0, CollectionStatus.QUERO_JOGAR)
            ));

            semearConversa(primeira, demo, critica);
            semearListas(demo, games);
        }
    }

    // Uma discussao de exemplo na primeira avaliacao: dois votos e uma thread de
    // tres niveis.
    //
    // Sem isto as telas novas nascem vazias, e "vazio" e indistinguivel de
    // "quebrado" pra quem abre o projeto pela primeira vez - inclusive pra quem
    // for avaliar o trabalho.
    private void semearConversa(Review alvo, User demo, User critica) {
        reviewVoteRepository.save(new ReviewVote(critica, alvo, VoteType.POSITIVE));

        ReviewReply raiz = reviewReplyRepository.save(new ReviewReply(
                alvo, critica, null, "Discordo do 5, mas entendo o entusiasmo."));
        ReviewReply resposta = reviewReplyRepository.save(new ReviewReply(
                alvo, demo, raiz, "@critica o que te incomodou?"));
        reviewReplyRepository.save(new ReviewReply(
                alvo, critica, resposta, "A repeticao das side quests, principalmente."));
    }

    // Duas listas do demo: uma publica com tags e notas por jogo, outra privada.
    //
    // A privada existe pra deixar a regra de visibilidade observavel: aberta
    // deslogado, a mesma URL responde 404.
    private void semearListas(User demo, List<Game> games) {
        GameList favoritos = new GameList(demo, "Valeram cada hora",
                "Os que eu recomendaria pra qualquer pessoa, sem perguntar o que ela costuma jogar.");
        favoritos.setTags(Set.of("favoritos", "pra comecar"));
        favoritos.addItem(games.get(0), "Esse aqui e o motivo da lista existir.");
        favoritos.addItem(games.get(1), null);
        gameListRepository.save(favoritos);

        GameList privada = new GameList(demo, "Comprar quando baixar",
                "Lista de espera de promocao.");
        privada.setTags(Set.of("promocao"));
        privada.setVisibility(ListVisibility.PRIVATE);
        privada.addItem(games.get(2), null);
        gameListRepository.save(privada);
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
