package com.gamelog.list.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.list.domain.ListVisibility;
import com.gamelog.list.dto.AddListItemRequest;
import com.gamelog.list.dto.GameListResponse;
import com.gamelog.list.dto.GameListSummary;
import com.gamelog.list.dto.SaveGameListRequest;
import com.gamelog.list.repository.GameListRepository;
import com.gamelog.shared.BadRequestException;
import com.gamelog.shared.NotFoundException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// As regras das listas tematicas.
//
// A que mais importa e a de visibilidade: uma lista privada tem que responder 404
// pra quem nao e o dono. Devolver 403 seria confirmar que a lista existe - e "essa
// lista existe mas voce nao pode ver" ja e informacao sobre alguem que marcou
// aquilo como so seu.
@DataJpaTest
class GameListServiceTest {

    @Autowired
    private GameListRepository gameListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private GameListService service;
    private Game zelda;
    private Game hades;

    @BeforeEach
    void seed() {
        service = new GameListService(gameListRepository, userRepository, gameRepository);

        userRepository.save(new User("ana", "ana@email.com", "hash", null));
        userRepository.save(new User("beto", "beto@email.com", "hash", null));

        zelda = gameRepository.save(new Game(1101L, "Zelda", null, 2017, "Aventura", "url"));
        hades = gameRepository.save(new Game(1102L, "Hades", null, 2020, "Roguelike", "url"));
    }

    private SaveGameListRequest pedido(String titulo, Set<String> tags, ListVisibility visibilidade) {
        return new SaveGameListRequest(titulo, "descricao", null, tags, visibilidade);
    }

    // ---------- criar e editar ----------

    @Test
    void criaUmaListaComTags() {
        GameListResponse lista = service.create("ana", pedido("Favoritos", Set.of("indie"), null));

        assertThat(lista.title()).isEqualTo("Favoritos");
        assertThat(lista.tags()).containsExactly("indie");
        assertThat(lista.owner()).isEqualTo("ana");
        // Sem visibilidade informada, nasce publica - como o resto do app.
        assertThat(lista.visibility()).isEqualTo("PUBLIC");
    }

    @Test
    void recusaMaisDeCincoTags() {
        // Recusar, e nao cortar em silencio: quem digitou seis tags precisa saber
        // que a sexta nao entrou, senao vai procura-la depois e nao vai achar.
        SaveGameListRequest seisTags = pedido(
                "Demais", Set.of("a", "b", "c", "d", "e", "f"), null);

        assertThatThrownBy(() -> service.create("ana", seisTags))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("5");
    }

    @Test
    void recusaCapaComEsquemaPerigoso() {
        SaveGameListRequest comScript = new SaveGameListRequest(
                "Lista", null, "javascript:alert(1)", Set.of(), null);

        assertThatThrownBy(() -> service.create("ana", comScript))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void soODonoEdita() {
        GameListResponse daAna = service.create("ana", pedido("Da ana", Set.of(), null));

        assertThatThrownBy(() -> service.update("beto", daAna.id(), pedido("Roubada", Set.of(), null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void soODonoApaga() {
        GameListResponse daAna = service.create("ana", pedido("Da ana", Set.of(), null));

        assertThatThrownBy(() -> service.delete("beto", daAna.id()))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- visibilidade ----------

    @Test
    void listaPrivadaResponde404ParaQuemNaoEDono() {
        GameListResponse privada = service.create(
                "ana", pedido("Segredo", Set.of(), ListVisibility.PRIVATE));

        // 404 e nao 403: negar permissao confirmaria que a lista existe.
        assertThatThrownBy(() -> service.findById(privada.id(), "beto"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listaPrivadaResponde404ParaVisitanteDeslogado() {
        GameListResponse privada = service.create(
                "ana", pedido("Segredo", Set.of(), ListVisibility.PRIVATE));

        assertThatThrownBy(() -> service.findById(privada.id(), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void oDonoEnxergaAPropriaListaPrivada() {
        GameListResponse privada = service.create(
                "ana", pedido("Segredo", Set.of(), ListVisibility.PRIVATE));

        assertThat(service.findById(privada.id(), "ana").title()).isEqualTo("Segredo");
    }

    @Test
    void oPerfilAlheioMostraSoAsListasPublicas() {
        service.create("ana", pedido("Publica", Set.of(), null));
        service.create("ana", pedido("Privada", Set.of(), ListVisibility.PRIVATE));

        List<GameListSummary> paraOBeto = service.findByOwner("ana", "beto");
        List<GameListSummary> paraAAna = service.findByOwner("ana", "ana");

        assertThat(paraOBeto).extracting(GameListSummary::title).containsExactly("Publica");
        // O dono ve as duas no proprio perfil.
        assertThat(paraAAna).hasSize(2);
    }

    // ---------- itens ----------

    @Test
    void adicionaUmJogoComNota() {
        GameListResponse lista = service.create("ana", pedido("Favoritos", Set.of(), null));

        GameListResponse comJogo = service.addItem(
                "ana", lista.id(), new AddListItemRequest(zelda.getId(), "o motivo da lista existir"));

        assertThat(comJogo.items()).hasSize(1);
        assertThat(comJogo.items().get(0).note()).isEqualTo("o motivo da lista existir");
        assertThat(comJogo.items().get(0).gameTitle()).isEqualTo("Zelda");
    }

    @Test
    void recusaOMesmoJogoDuasVezes() {
        GameListResponse lista = service.create("ana", pedido("Favoritos", Set.of(), null));
        service.addItem("ana", lista.id(), new AddListItemRequest(zelda.getId(), null));

        assertThatThrownBy(() -> service.addItem(
                "ana", lista.id(), new AddListItemRequest(zelda.getId(), "de novo")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void recusaJogoQueNaoExiste() {
        GameListResponse lista = service.create("ana", pedido("Favoritos", Set.of(), null));

        assertThatThrownBy(() -> service.addItem(
                "ana", lista.id(), new AddListItemRequest(99999L, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void soODonoAdicionaJogo() {
        GameListResponse daAna = service.create("ana", pedido("Da ana", Set.of(), null));

        assertThatThrownBy(() -> service.addItem(
                "beto", daAna.id(), new AddListItemRequest(zelda.getId(), null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void tirarUmJogoRenumeraOsQueSobraram() {
        // Sem renumerar sobrariam buracos (0, 2), e o proximo jogo adicionado
        // entraria numa posicao ja ocupada - dois itens empatados, com a ordem
        // decidida pelo banco.
        GameListResponse lista = service.create("ana", pedido("Favoritos", Set.of(), null));
        GameListResponse comDois = service.addItem(
                "ana", lista.id(), new AddListItemRequest(zelda.getId(), null));
        service.addItem("ana", lista.id(), new AddListItemRequest(hades.getId(), null));

        Long primeiroItem = comDois.items().get(0).id();
        GameListResponse depois = service.removeItem("ana", lista.id(), primeiroItem);

        assertThat(depois.items()).hasSize(1);
        assertThat(depois.items().get(0).position()).isZero();
    }

    @Test
    void editaANotaDeUmJogoDaLista() {
        GameListResponse lista = service.create("ana", pedido("Favoritos", Set.of(), null));
        GameListResponse comJogo = service.addItem(
                "ana", lista.id(), new AddListItemRequest(zelda.getId(), "primeira ideia"));

        GameListResponse editada = service.updateItemNote(
                "ana", lista.id(), comJogo.items().get(0).id(), "pensando melhor");

        assertThat(editada.items().get(0).note()).isEqualTo("pensando melhor");
    }

    @Test
    void naoDeixaMexerNumItemDeOutraLista() {
        // Sem esta checagem, mandar o id de um item alheio junto do id da minha
        // lista deixaria editar a nota de qualquer pessoa.
        GameListResponse daAna = service.create("ana", pedido("Da ana", Set.of(), null));
        GameListResponse comJogo = service.addItem(
                "ana", daAna.id(), new AddListItemRequest(zelda.getId(), null));

        GameListResponse outraDaAna = service.create("ana", pedido("Outra", Set.of(), null));

        assertThatThrownBy(() -> service.updateItemNote(
                "ana", outraDaAna.id(), comJogo.items().get(0).id(), "invasao"))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- descoberta ----------

    @Test
    void achaListasPublicasPorTag() {
        service.create("ana", pedido("Indies", Set.of("indie"), null));
        service.create("beto", pedido("Terror", Set.of("terror"), null));

        assertThat(service.findByTag("indie")).extracting(GameListSummary::title)
                .containsExactly("Indies");
    }

    @Test
    void aBuscaPorTagNormalizaOTermo() {
        // Quem digita "INDIE" na busca quer as mesmas listas de quem digita
        // "indie" - as tags sao guardadas em minusculas.
        service.create("ana", pedido("Indies", Set.of("indie"), null));

        assertThat(service.findByTag("  INDIE ")).hasSize(1);
    }

    @Test
    void oResumoTrazAQuantidadeDeJogos() {
        GameListResponse lista = service.create("ana", pedido("Favoritos", Set.of(), null));
        service.addItem("ana", lista.id(), new AddListItemRequest(zelda.getId(), null));
        service.addItem("ana", lista.id(), new AddListItemRequest(hades.getId(), null));
        service.create("ana", pedido("Vazia", Set.of(), null));

        List<GameListSummary> resumos = service.findByOwner("ana", "ana");

        assertThat(resumos).extracting(GameListSummary::title, GameListSummary::gameCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Favoritos", 2L),
                        // Lista vazia nao aparece na consulta de contagem; tem que
                        // sair como zero, e nao sumir do perfil.
                        org.assertj.core.groups.Tuple.tuple("Vazia", 0L));
    }
}
