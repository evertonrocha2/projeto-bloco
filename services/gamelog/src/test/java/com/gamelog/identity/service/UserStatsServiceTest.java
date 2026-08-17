package com.gamelog.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

// A faixa de numeros no topo do perfil, as conquistas e a retrospectiva do ano.
//
// Tudo DERIVADO na leitura, sem tabela nova. Guardar contadores criaria a classe
// de bug mais chata que existe: o numero que discorda da lista logo abaixo dele,
// porque um caminho de escrita esqueceu de incrementar.
//
// O relogio e injetado. Sem isso a retrospectiva do ano so seria testavel em
// dezembro, ou o teste passaria a falhar sozinho na virada do ano.
@DataJpaTest
class UserStatsServiceTest {

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private GameListRepository gameListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UserStatsService service;
    private User ana;
    private Game zelda;
    private Game hades;
    private Game celeste;

    // 15 de junho de 2026, meio-dia UTC.
    private static final Instant AGORA = Instant.parse("2026-06-15T12:00:00Z");
    private static final Instant NO_ANO = Instant.parse("2026-03-10T12:00:00Z");
    private static final Instant ANO_PASSADO = Instant.parse("2025-11-20T12:00:00Z");

    @BeforeEach
    void seed() {
        service = new UserStatsService(
                collectionRepository, reviewRepository, gameListRepository, userRepository,
                Clock.fixed(AGORA, ZoneId.of("UTC")));

        ana = userRepository.save(new User("ana", "ana@email.com", "hash", null));

        zelda = gameRepository.save(new Game(1201L, "Zelda", null, 2017, "Aventura, RPG", "url"));
        hades = gameRepository.save(new Game(1202L, "Hades", null, 2020, "Roguelike", "url"));
        celeste = gameRepository.save(new Game(1203L, "Celeste", null, 2018, "RPG", "url"));
    }

    private CollectionEntry entrada(Game game, int horas, CollectionStatus status) {
        return collectionRepository.save(new CollectionEntry(ana, game, horas, status));
    }

    @Test
    void somaAsHorasDaColecaoInteira() {
        entrada(zelda, 120, CollectionStatus.ZERADO);
        entrada(hades, 40, CollectionStatus.JOGANDO);

        assertThat(service.forUser("ana").totalHours()).isEqualTo(160);
    }

    @Test
    void contaQuantosJogosEmCadaStatus() {
        entrada(zelda, 120, CollectionStatus.PLATINADO);
        entrada(hades, 40, CollectionStatus.ZERADO);
        entrada(celeste, 5, CollectionStatus.ZERADO);

        UserStatsResponse stats = service.forUser("ana");

        assertThat(stats.countByStatus())
                .containsEntry("PLATINADO", 1L)
                .containsEntry("ZERADO", 2L);
    }

    @Test
    void statusSemNenhumJogoSaiComoZero() {
        // As abas do perfil mostram todos os cinco status, sempre. Um status
        // ausente do mapa faria a aba renderizar "undefined" no lugar do numero.
        entrada(zelda, 10, CollectionStatus.JOGANDO);

        assertThat(service.forUser("ana").countByStatus())
                .containsEntry("QUERO_JOGAR", 0L)
                .containsEntry("LARGADO", 0L)
                .hasSize(5);
    }

    @Test
    void oGeneroFavoritoSeparaOCampoComVariosGeneros() {
        // Game.genre guarda "Aventura, RPG" numa string so. Sem separar, esse par
        // viraria um genero proprio chamado "Aventura, RPG" - que nao e o genero
        // favorito de ninguem.
        entrada(zelda, 10, CollectionStatus.ZERADO);    // Aventura, RPG
        entrada(celeste, 10, CollectionStatus.ZERADO);  // RPG

        // RPG aparece duas vezes; Aventura, uma.
        assertThat(service.forUser("ana").favoriteGenre()).isEqualTo("RPG");
    }

    @Test
    void oGeneroFavoritoDesempataPorOrdemAlfabetica() {
        // Sem criterio de desempate o resultado dependeria da ordem em que o banco
        // devolveu as linhas, e mudaria de uma requisicao pra outra sem nada ter
        // mudado na colecao.
        entrada(hades, 10, CollectionStatus.ZERADO);    // Roguelike
        entrada(celeste, 10, CollectionStatus.ZERADO);  // RPG

        assertThat(service.forUser("ana").favoriteGenre()).isEqualTo("RPG");
    }

    @Test
    void semColecaoNaoHaGeneroFavorito() {
        assertThat(service.forUser("ana").favoriteGenre()).isNull();
    }

    @Test
    void calculaANotaMediaQueAPessoaDa() {
        reviewRepository.save(new Review(ana, zelda, 5, "otimo"));
        reviewRepository.save(new Review(ana, hades, 3, "ok"));

        assertThat(service.forUser("ana").averageRatingGiven()).isEqualTo(4.0);
    }

    // ---------- retrospectiva ----------

    @Test
    void aRetrospectivaContaSoOQueFoiTerminadoNoAno() {
        CollectionEntry doAno = entrada(zelda, 120, CollectionStatus.ZERADO);
        CollectionEntry doAnoPassado = entrada(hades, 40, CollectionStatus.ZERADO);
        forcarConclusao(doAno, NO_ANO);
        forcarConclusao(doAnoPassado, ANO_PASSADO);

        UserStatsResponse stats = service.forUser("ana");

        assertThat(stats.year()).isEqualTo(2026);
        assertThat(stats.finishedThisYear()).isEqualTo(1);
    }

    @Test
    void jogoNaoTerminadoFicaForaDaRetrospectiva() {
        entrada(zelda, 200, CollectionStatus.JOGANDO);

        assertThat(service.forUser("ana").finishedThisYear()).isZero();
    }

    @Test
    void oMaisJogadoDoAnoEOQueTemMaisHorasEntreOsTerminados() {
        CollectionEntry longo = entrada(zelda, 120, CollectionStatus.ZERADO);
        CollectionEntry curto = entrada(celeste, 12, CollectionStatus.PLATINADO);
        forcarConclusao(longo, NO_ANO);
        forcarConclusao(curto, NO_ANO);

        assertThat(service.forUser("ana").mostPlayedThisYear()).isEqualTo("Zelda");
    }

    @Test
    void oMelhorAvaliadoDoAnoEAMaiorNotaQueVoceDeu() {
        reviewRepository.save(new Review(ana, zelda, 3, "ok"));
        reviewRepository.save(new Review(ana, hades, 5, "obra prima"));

        assertThat(service.forUser("ana").bestRatedThisYear()).isEqualTo("Hades");
    }

    // ---------- conquistas ----------

    @Test
    void aPrimeiraPlatinaDestravaUmaConquista() {
        entrada(zelda, 120, CollectionStatus.PLATINADO);

        assertThat(service.forUser("ana").achievements()).contains("FIRST_PLATINUM");
    }

    @Test
    void semPlatinaNaoHaConquistaDePlatina() {
        entrada(zelda, 120, CollectionStatus.ZERADO);

        assertThat(service.forUser("ana").achievements()).doesNotContain("FIRST_PLATINUM");
    }

    @Test
    void dezJogosTerminadosDestravamAConquista() {
        // Zerados e platinados contam juntos: os dois sao "terminei".
        for (int i = 0; i < 10; i++) {
            Game jogo = gameRepository.save(
                    new Game(1300L + i, "Jogo " + i, null, 2020, "RPG", "url"));
            entrada(jogo, 10, CollectionStatus.ZERADO);
        }

        assertThat(service.forUser("ana").achievements()).contains("TEN_FINISHED");
    }

    @Test
    void noveJogosTerminadosAindaNaoDestravam() {
        // O limite tem que ser exatamente onde diz que e, senao a conquista
        // aparece antes da hora e perde a graca.
        for (int i = 0; i < 9; i++) {
            Game jogo = gameRepository.save(
                    new Game(1400L + i, "Jogo " + i, null, 2020, "RPG", "url"));
            entrada(jogo, 10, CollectionStatus.ZERADO);
        }

        assertThat(service.forUser("ana").achievements()).doesNotContain("TEN_FINISHED");
    }

    @Test
    void umaListaComVinteJogosDestravaAConquista() {
        GameList lista = gameListRepository.save(new GameList(ana, "Enorme", null));
        for (int i = 0; i < 20; i++) {
            Game jogo = gameRepository.save(
                    new Game(1500L + i, "Jogo " + i, null, 2020, "RPG", "url"));
            lista.addItem(jogo, null);
        }
        gameListRepository.saveAndFlush(lista);

        assertThat(service.forUser("ana").achievements()).contains("LIST_OF_TWENTY");
    }

    @Test
    void duasListasDeDezNaoDestravamAConquista() {
        // A conquista e por UMA lista de vinte, nao por vinte itens espalhados.
        for (int lista = 0; lista < 2; lista++) {
            GameList atual = gameListRepository.save(new GameList(ana, "Lista " + lista, null));
            for (int i = 0; i < 10; i++) {
                Game jogo = gameRepository.save(
                        new Game(1600L + lista * 100 + i, "Jogo", null, 2020, "RPG", "url"));
                atual.addItem(jogo, null);
            }
            gameListRepository.saveAndFlush(atual);
        }

        assertThat(service.forUser("ana").achievements()).doesNotContain("LIST_OF_TWENTY");
    }

    @Test
    void perfilVazioNaoQuebraENaoTemConquista() {
        UserStatsResponse stats = service.forUser("ana");

        assertThat(stats.totalHours()).isZero();
        assertThat(stats.averageRatingGiven()).isZero();
        assertThat(stats.achievements()).isEmpty();
        assertThat(stats.mostPlayedThisYear()).isNull();
    }

    // finishedAt e gravado pelo dominio com Instant.now(), que e o comportamento
    // correto em producao. Pra testar a retrospectiva a data precisa ser
    // controlada, e a alternativa seria fazer CollectionEntry receber um Clock -
    // arrastar uma dependencia pro dominio inteiro pra atender um teste.
    //
    // Aqui a data e reescrita direto pelo EntityManager. O truque fica NO TESTE,
    // que e onde ele pertence: um metodo de repositorio existindo so pra isto
    // seria codigo de teste morando em producao, disponivel pra qualquer um
    // chamar sem querer.
    private void forcarConclusao(CollectionEntry entry, Instant quando) {
        entityManager.flush();
        entityManager.getEntityManager()
                .createQuery("update CollectionEntry ce set ce.finishedAt = :quando where ce.id = :id")
                .setParameter("quando", quando)
                .setParameter("id", entry.getId())
                .executeUpdate();
        entityManager.clear();
    }
}
