package com.gamelog.identity.service;

import com.gamelog.collection.domain.CollectionEntry;
import com.gamelog.collection.domain.CollectionStatus;
import com.gamelog.collection.repository.CollectionRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.dto.UserStatsResponse;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.list.domain.GameList;
import com.gamelog.list.repository.GameListRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.shared.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Os numeros do perfil.
//
// Tudo derivado na leitura, sem tabela nova e sem contador guardado. Contadores
// persistidos produzem o bug mais chato de diagnosticar que existe: o numero que
// discorda da lista logo abaixo dele, porque um caminho de escrita esqueceu de
// incrementar. Aqui o numero e a lista saem do mesmo lugar, sempre.
@Service
public class UserStatsService {

    // Quantos jogos terminados destravam a conquista de dez.
    private static final int TEN_FINISHED = 10;

    // Tamanho de lista que destrava a conquista de colecionador.
    private static final int BIG_LIST_SIZE = 20;

    private static final int REVIEWS_FOR_CRITIC = 25;

    private final CollectionRepository collectionRepository;
    private final ReviewRepository reviewRepository;
    private final GameListRepository gameListRepository;
    private final UserRepository userRepository;

    // O relogio e injetado, e nao Instant.now() solto no meio do metodo.
    //
    // A retrospectiva depende de "que ano e hoje". Com o relogio do sistema, o
    // teste dela so passaria no ano em que foi escrito, e comecaria a falhar
    // sozinho na virada - um teste que quebra sem ninguem ter mexido em nada e um
    // teste que a equipe aprende a ignorar.
    private final Clock clock;

    public UserStatsService(CollectionRepository collectionRepository,
                            ReviewRepository reviewRepository,
                            GameListRepository gameListRepository,
                            UserRepository userRepository,
                            Clock clock) {
        this.collectionRepository = collectionRepository;
        this.reviewRepository = reviewRepository;
        this.gameListRepository = gameListRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserStatsResponse forUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));

        List<CollectionEntry> collection =
                collectionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        int year = LocalDate.now(clock).getYear();
        List<CollectionEntry> terminadosNoAno = terminadosEm(collection, year);

        return new UserStatsResponse(
                somarHoras(collection),
                contarPorStatus(user.getId()),
                generoFavorito(collection),
                notaMedia(reviews),
                year,
                terminadosNoAno.size(),
                maisJogado(terminadosNoAno),
                melhorAvaliado(reviews, year),
                conquistas(user, collection, reviews)
        );
    }

    private int somarHoras(List<CollectionEntry> collection) {
        return collection.stream().mapToInt(CollectionEntry::getHoursPlayed).sum();
    }

    // Quantos jogos em cada status.
    //
    // A contagem vem agrupada do banco (uma consulta), mas o mapa e preenchido
    // com os CINCO status sempre - inclusive os que deram zero. As abas do perfil
    // mostram todos, e uma chave ausente faria a aba escrever "undefined" onde
    // deveria haver um numero.
    private Map<String, Long> contarPorStatus(Long userId) {
        Map<String, Long> contagens = new LinkedHashMap<>();
        for (CollectionStatus status : CollectionStatus.values()) {
            contagens.put(status.name(), 0L);
        }

        for (Object[] linha : collectionRepository.countByStatusForUser(userId)) {
            contagens.put(((CollectionStatus) linha[0]).name(), (Long) linha[1]);
        }

        return contagens;
    }

    // O genero que mais aparece na colecao.
    //
    // Game.genre guarda varios generos numa string so ("Aventura, RPG"), do jeito
    // que a API da RAWG devolve. Sem separar, esse par viraria um genero proprio
    // chamado "Aventura, RPG" - que nao e o genero favorito de ninguem. E a mesma
    // regra do lib/genres.js do front.
    private String generoFavorito(List<CollectionEntry> collection) {
        Map<String, Integer> frequencia = new HashMap<>();

        for (CollectionEntry entry : collection) {
            for (String genero : separarGeneros(entry.getGame().getGenre())) {
                frequencia.merge(genero, 1, Integer::sum);
            }
        }

        // Desempate alfabetico. Sem criterio, o resultado dependeria da ordem em
        // que o banco devolveu as linhas e mudaria de uma requisicao pra outra sem
        // nada ter mudado na colecao.
        return frequencia.entrySet().stream()
                .max(Comparator
                        .<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private List<String> separarGeneros(String campo) {
        if (campo == null || campo.isBlank()) {
            return List.of();
        }

        List<String> generos = new ArrayList<>();
        for (String pedaco : campo.split(",")) {
            String genero = pedaco.trim();
            if (!genero.isEmpty()) {
                generos.add(genero);
            }
        }

        return generos;
    }

    private double notaMedia(List<Review> reviews) {
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0);
    }

    // ---------- retrospectiva ----------

    // Os jogos terminados no ano, pela data de conclusao - e nao pela data de
    // entrada na colecao, que e outra coisa (ver o comentario em
    // CollectionEntry.finishedAt).
    private List<CollectionEntry> terminadosEm(List<CollectionEntry> collection, int year) {
        return collection.stream()
                .filter(entry -> entry.getFinishedAt() != null)
                .filter(entry -> anoDe(entry.getFinishedAt()) == year)
                .toList();
    }

    private String maisJogado(List<CollectionEntry> terminadosNoAno) {
        return terminadosNoAno.stream()
                .max(Comparator.comparingInt(CollectionEntry::getHoursPlayed))
                .map(entry -> entry.getGame().getTitle())
                .orElse(null);
    }

    // A maior nota que a pessoa deu no ano. Empate fica com a mais recente - as
    // reviews ja chegam da mais nova pra mais antiga.
    private String melhorAvaliado(List<Review> reviews, int year) {
        return reviews.stream()
                .filter(review -> anoDe(review.getCreatedAt()) == year)
                .max(Comparator.comparingInt(Review::getRating))
                .map(review -> review.getGame().getTitle())
                .orElse(null);
    }

    private int anoDe(Instant instante) {
        return instante.atZone(clock.getZone()).getYear();
    }

    // ---------- conquistas ----------

    private List<String> conquistas(User user,
                                    List<CollectionEntry> collection,
                                    List<Review> reviews) {
        List<String> ganhas = new ArrayList<>();

        boolean temPlatina = collection.stream()
                .anyMatch(entry -> entry.getStatus() == CollectionStatus.PLATINADO);
        if (temPlatina) {
            ganhas.add("FIRST_PLATINUM");
        }

        // Zerado e platinado contam juntos: os dois querem dizer "terminei".
        long terminados = collection.stream()
                .filter(entry -> entry.getStatus() == CollectionStatus.ZERADO
                        || entry.getStatus() == CollectionStatus.PLATINADO)
                .count();
        if (terminados >= TEN_FINISHED) {
            ganhas.add("TEN_FINISHED");
        }

        if (reviews.size() >= REVIEWS_FOR_CRITIC) {
            ganhas.add("TWENTY_FIVE_REVIEWS");
        }

        if (temListaGrande(user)) {
            ganhas.add("LIST_OF_TWENTY");
        }

        return ganhas;
    }

    // A conquista e por UMA lista de vinte, e nao por vinte itens espalhados em
    // varias - montar uma lista longa e sobre curadoria, nao sobre volume.
    private boolean temListaGrande(User user) {
        List<GameList> lists = gameListRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
        if (lists.isEmpty()) {
            return false;
        }

        return gameListRepository
                .countItemsByListIds(lists.stream().map(GameList::getId).toList())
                .stream()
                .anyMatch(linha -> (Long) linha[1] >= BIG_LIST_SIZE);
    }
}
