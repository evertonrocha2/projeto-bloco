package com.gamelog.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.catalog.domain.Game;
import com.gamelog.identity.domain.User;
import org.junit.jupiter.api.Test;

// Quando um jogo foi TERMINADO.
//
// A retrospectiva do ano precisa responder "o que voce zerou em 2026", e nenhum
// campo existente serve pra isso. createdAt e quando o jogo entrou na colecao -
// alguem que adicionou um jogo em janeiro e zerou em dezembro apareceria na
// retrospectiva errada. updatedAt tambem nao: corrigir as horas jogadas depois
// moveria a data de conclusao junto.
//
// A alternativa era varrer o historico do Envers atras da revisao em que o status
// virou ZERADO. Correto, mas seria uma consulta por item da colecao pra montar um
// bloco de perfil. Uma coluna anulavel, gravada na transicao, resolve.
class CollectionFinishedAtTest {

    private final User ana = new User("ana", "ana@email.com", "hash", null);
    private final Game zelda = new Game(901L, "Zelda", null, 2017, "Aventura", "url");

    @Test
    void jogoNaoTerminadoNaoTemDataDeConclusao() {
        CollectionEntry entry = new CollectionEntry(ana, zelda, 0, CollectionStatus.QUERO_JOGAR);

        assertThat(entry.getFinishedAt()).isNull();
    }

    @Test
    void nascerZeradoJaGravaADataDeConclusao() {
        // Caso real: a pessoa adiciona um jogo que ja tinha terminado antes de
        // usar o app.
        CollectionEntry entry = new CollectionEntry(ana, zelda, 60, CollectionStatus.ZERADO);

        assertThat(entry.getFinishedAt()).isNotNull();
    }

    @Test
    void platinarTambemConta() {
        // Platinado e um estado terminal como zerado - quem platinou terminou.
        CollectionEntry entry = new CollectionEntry(ana, zelda, 90, CollectionStatus.PLATINADO);

        assertThat(entry.getFinishedAt()).isNotNull();
    }

    @Test
    void terminarUmJogoEmAndamentoGravaAData() {
        CollectionEntry entry = new CollectionEntry(ana, zelda, 10, CollectionStatus.JOGANDO);

        entry.setStatus(CollectionStatus.ZERADO);

        assertThat(entry.getFinishedAt()).isNotNull();
    }

    @Test
    void zeradoQueViraPlatinadoMantemAPrimeiraData() {
        // Quem zera e depois volta pra platinar terminou o jogo UMA vez. Regravar
        // a data na segunda transicao mudaria o ano da conquista se a platina
        // viesse em janeiro seguinte.
        CollectionEntry entry = new CollectionEntry(ana, zelda, 60, CollectionStatus.ZERADO);
        var primeira = entry.getFinishedAt();

        entry.setStatus(CollectionStatus.PLATINADO);

        assertThat(entry.getFinishedAt()).isEqualTo(primeira);
    }

    @Test
    void voltarPraJogandoApagaADataDeConclusao() {
        // Desmarcar como zerado significa que o jogo nao esta terminado. Manter a
        // data deixaria o jogo na retrospectiva de "zerados do ano" sem estar
        // zerado - um numero que a pessoa nao consegue explicar olhando a lista.
        CollectionEntry entry = new CollectionEntry(ana, zelda, 60, CollectionStatus.ZERADO);

        entry.setStatus(CollectionStatus.JOGANDO);

        assertThat(entry.getFinishedAt()).isNull();
    }

    @Test
    void largarUmJogoZeradoTambemApagaAData() {
        CollectionEntry entry = new CollectionEntry(ana, zelda, 60, CollectionStatus.ZERADO);

        entry.setStatus(CollectionStatus.LARGADO);

        assertThat(entry.getFinishedAt()).isNull();
    }
}
