package com.gamelog.list.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.list.domain.GameList;
import com.gamelog.list.domain.GameListItem;
import com.gamelog.list.domain.ListVisibility;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

// Listas tematicas: "os que valeram cada hora", "os que eu larguei no tutorial".
//
// E uma relacao diferente da colecao, e por isso mora em outra tabela. Na
// colecao um jogo tem UM status; numa lista o mesmo jogo pode aparecer em varias
// listas ao mesmo tempo. Sao regras opostas, e junta-las obrigaria uma das duas a
// ceder.
@DataJpaTest
class GameListRepositoryTest {

    @Autowired
    private GameListRepository gameListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private User ana;
    private User beto;
    private Game zelda;
    private Game hades;

    @BeforeEach
    void seed() {
        ana = userRepository.save(new User("ana", "ana@email.com", "hash", null));
        beto = userRepository.save(new User("beto", "beto@email.com", "hash", null));

        zelda = gameRepository.save(new Game(1001L, "Zelda", null, 2017, "Aventura", "url"));
        hades = gameRepository.save(new Game(1002L, "Hades", null, 2020, "Roguelike", "url"));
    }

    @Test
    void guardaUmaListaComTagsEDescricao() {
        GameList lista = new GameList(ana, "Valeram cada hora", "os que eu recomendaria pra qualquer um");
        lista.setTags(Set.of("indie", "favoritos"));

        GameList salva = gameListRepository.save(lista);

        assertThat(salva.getTitle()).isEqualTo("Valeram cada hora");
        assertThat(salva.getTags()).containsExactlyInAnyOrder("indie", "favoritos");
        // Publica por padrao: o app nao tem nada privado hoje, e a lista so vira
        // privada se a pessoa pedir.
        assertThat(salva.getVisibility()).isEqualTo(ListVisibility.PUBLIC);
    }

    @Test
    void normalizaAsTags() {
        // Sem normalizar, "Indie", "indie" e " indie " viram tres tags distintas e
        // o filtro por tag passa a depender de a pessoa ter digitado igual.
        GameList lista = new GameList(ana, "Lista", null);
        lista.setTags(Set.of("Indie", "  TERROR  "));

        GameList salva = gameListRepository.save(lista);

        assertThat(salva.getTags()).containsExactlyInAnyOrder("indie", "terror");
    }

    @Test
    void descartaTagVazia() {
        GameList lista = new GameList(ana, "Lista", null);
        lista.setTags(Set.of("indie", "   ", ""));

        assertThat(gameListRepository.save(lista).getTags()).containsExactly("indie");
    }

    @Test
    void listaAsListasDeUmDono() {
        gameListRepository.save(new GameList(ana, "Primeira", null));
        gameListRepository.save(new GameList(ana, "Segunda", null));
        gameListRepository.save(new GameList(beto, "Do beto", null));

        assertThat(gameListRepository.findByOwnerIdOrderByCreatedAtDesc(ana.getId())).hasSize(2);
    }

    @Test
    void listaSoAsPublicasDeUmDono() {
        // E o que um visitante ve no perfil de outra pessoa.
        gameListRepository.save(new GameList(ana, "Publica", null));
        GameList privada = new GameList(ana, "Privada", null);
        privada.setVisibility(ListVisibility.PRIVATE);
        gameListRepository.save(privada);

        List<GameList> publicas = gameListRepository
                .findByOwnerIdAndVisibilityOrderByCreatedAtDesc(ana.getId(), ListVisibility.PUBLIC);

        assertThat(publicas).hasSize(1);
        assertThat(publicas.get(0).getTitle()).isEqualTo("Publica");
    }

    @Test
    void achaListasPublicasPorTag() {
        GameList comTag = new GameList(ana, "Indies", null);
        comTag.setTags(Set.of("indie"));
        gameListRepository.save(comTag);

        GameList outraTag = new GameList(beto, "Terror", null);
        outraTag.setTags(Set.of("terror"));
        gameListRepository.save(outraTag);

        List<GameList> achadas = gameListRepository.findPublicByTag("indie");

        assertThat(achadas).hasSize(1);
        assertThat(achadas.get(0).getTitle()).isEqualTo("Indies");
    }

    @Test
    void aBuscaPorTagNaoVazaListaPrivada() {
        // A lista privada tem a tag, mas nao pode aparecer numa tela de descoberta
        // - e o caminho mais facil de vazar conteudo que alguem marcou como so seu.
        GameList privada = new GameList(ana, "Privada com tag", null);
        privada.setTags(Set.of("indie"));
        privada.setVisibility(ListVisibility.PRIVATE);
        gameListRepository.save(privada);

        assertThat(gameListRepository.findPublicByTag("indie")).isEmpty();
    }

    @Test
    void guardaOsJogosDaListaComNotaEPosicao() {
        GameList lista = gameListRepository.save(new GameList(ana, "Favoritos", null));

        lista.addItem(zelda, "o motivo da lista existir");
        lista.addItem(hades, null);
        GameList salva = gameListRepository.saveAndFlush(lista);

        assertThat(salva.getItems()).hasSize(2);
        assertThat(salva.getItems().get(0).getNote()).isEqualTo("o motivo da lista existir");
        // A posicao vem da ordem de entrada, comecando em zero.
        assertThat(salva.getItems()).extracting(GameListItem::getPosition).containsExactly(0, 1);
    }

    @Test
    void recusaOMesmoJogoDuasVezesNaMesmaLista() {
        GameList lista = gameListRepository.save(new GameList(ana, "Favoritos", null));
        lista.addItem(zelda, null);
        gameListRepository.saveAndFlush(lista);

        lista.addItem(zelda, "de novo");

        assertThatThrownBy(() -> gameListRepository.saveAndFlush(lista))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void oMesmoJogoPodeEstarEmVariasListas() {
        // E a diferenca central pra colecao, onde um jogo tem um status so.
        GameList primeira = gameListRepository.save(new GameList(ana, "Favoritos", null));
        GameList segunda = gameListRepository.save(new GameList(ana, "Pra rejogar", null));

        primeira.addItem(zelda, null);
        segunda.addItem(zelda, null);
        gameListRepository.saveAndFlush(primeira);
        gameListRepository.saveAndFlush(segunda);

        assertThat(gameListRepository.findById(primeira.getId()).orElseThrow().getItems()).hasSize(1);
        assertThat(gameListRepository.findById(segunda.getId()).orElseThrow().getItems()).hasSize(1);
    }

    @Test
    void apagarAListaLevaOsItensJunto() {
        // Item de lista nao existe sozinho: sem a lista, ele nao significa nada.
        GameList lista = gameListRepository.save(new GameList(ana, "Temporaria", null));
        lista.addItem(zelda, null);
        gameListRepository.saveAndFlush(lista);

        gameListRepository.delete(lista);
        gameListRepository.flush();

        assertThat(gameListRepository.findById(lista.getId())).isEmpty();
    }

    @Test
    void contaOsJogosDeCadaListaNumaConsultaSo() {
        // O perfil mostra "12 jogos" em cada cartao de lista. Carregar os itens de
        // todas as listas so pra contar traria a colecao inteira pra memoria.
        GameList cheia = gameListRepository.save(new GameList(ana, "Cheia", null));
        cheia.addItem(zelda, null);
        cheia.addItem(hades, null);
        gameListRepository.saveAndFlush(cheia);

        GameList vazia = gameListRepository.save(new GameList(ana, "Vazia", null));
        gameListRepository.flush();

        List<Object[]> contagens = gameListRepository.countItemsByListIds(
                List.of(cheia.getId(), vazia.getId()));

        assertThat(contagens).hasSize(1);
        assertThat(contagens.get(0)[0]).isEqualTo(cheia.getId());
        assertThat(contagens.get(0)[1]).isEqualTo(2L);
    }
}
