package com.gamelog.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.review.domain.ReviewReply;
import com.gamelog.review.domain.VoteType;
import com.gamelog.review.dto.CreateReplyRequest;
import com.gamelog.review.dto.ReplyResponse;
import com.gamelog.review.dto.ReviewSocial;
import com.gamelog.review.repository.ReviewReplyRepository;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.review.repository.ReviewVoteRepository;
import com.gamelog.shared.BadRequestException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// As regras de voto e resposta.
//
// O service e montado a mao com os repositorios reais do @DataJpaTest, em vez de
// mockado: as regras aqui sao quase todas SOBRE o estado guardado - "ja existe um
// voto meu?", "essa resposta tem filhas?" - e um mock que responde o que eu mandar
// ele responder nao prova nada sobre nenhuma delas.
@DataJpaTest
class ReviewSocialServiceTest {

    @Autowired
    private ReviewVoteRepository reviewVoteRepository;

    @Autowired
    private ReviewReplyRepository reviewReplyRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private ReviewSocialService service;
    private Review reviewDaAna;

    @BeforeEach
    void seed() {
        service = new ReviewSocialService(
                reviewVoteRepository, reviewReplyRepository, reviewRepository, userRepository);

        User ana = userRepository.save(new User("ana", "ana@email.com", "hash", null));
        userRepository.save(new User("beto", "beto@email.com", "hash", null));
        userRepository.save(new User("carla", "carla@email.com", "hash", null));

        Game zelda = gameRepository.save(new Game(801L, "Zelda", null, 2017, "Aventura", "url"));
        reviewDaAna = reviewRepository.save(new Review(ana, zelda, 5, "obra prima"));
    }

    // ---------- votos ----------

    @Test
    void votarPositivoContaUmPositivo() {
        ReviewSocial resultado = service.vote("beto", reviewDaAna.getId(), VoteType.POSITIVE);

        assertThat(resultado.positiveVotes()).isEqualTo(1);
        assertThat(resultado.negativeVotes()).isZero();
        // A resposta ja diz de que lado quem chamou ficou, pra tela nao precisar
        // de uma segunda requisicao so pra saber se acende o polegar.
        assertThat(resultado.myVote()).isEqualTo("POSITIVE");
    }

    @Test
    void votarDeNovoNoMesmoLadoDesfazOVoto() {
        // E o clique que a pessoa espera que "desmarque". Sem isso, so daria pra
        // trocar de lado, nunca pra tirar o voto.
        service.vote("beto", reviewDaAna.getId(), VoteType.POSITIVE);
        ReviewSocial depois = service.vote("beto", reviewDaAna.getId(), VoteType.POSITIVE);

        assertThat(depois.positiveVotes()).isZero();
        assertThat(depois.myVote()).isNull();
        assertThat(reviewVoteRepository.findAll()).isEmpty();
    }

    @Test
    void votarNoLadoOpostoTrocaOVotoSemDuplicar() {
        service.vote("beto", reviewDaAna.getId(), VoteType.POSITIVE);
        ReviewSocial depois = service.vote("beto", reviewDaAna.getId(), VoteType.NEGATIVE);

        assertThat(depois.positiveVotes()).isZero();
        assertThat(depois.negativeVotes()).isEqualTo(1);
        // Uma linha so: trocar de lado edita o voto, nao cria outro.
        assertThat(reviewVoteRepository.findAll()).hasSize(1);
    }

    @Test
    void naoDeixaVotarNaPropriaAvaliacao() {
        // Sem essa regra, o placar viraria uma medida de quem se auto-elogia mais.
        assertThatThrownBy(() -> service.vote("ana", reviewDaAna.getId(), VoteType.POSITIVE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("propria");
    }

    @Test
    void removerVotoQueNaoExisteNaoQuebra() {
        // A rota de remover e idempotente: dois cliques rapidos nao podem
        // transformar o segundo num erro na cara da pessoa.
        service.removeVote("beto", reviewDaAna.getId());

        assertThat(reviewVoteRepository.findAll()).isEmpty();
    }

    @Test
    void votosDeGenteDiferenteSomam() {
        service.vote("beto", reviewDaAna.getId(), VoteType.POSITIVE);
        ReviewSocial resultado = service.vote("carla", reviewDaAna.getId(), VoteType.POSITIVE);

        assertThat(resultado.positiveVotes()).isEqualTo(2);
    }

    // ---------- respostas ----------

    @Test
    void respondeUmaAvaliacao() {
        ReplyResponse resposta = service.reply(
                "beto", reviewDaAna.getId(), new CreateReplyRequest("discordo do final", null));

        assertThat(resposta.username()).isEqualTo("beto");
        assertThat(resposta.text()).isEqualTo("discordo do final");
        assertThat(resposta.depth()).isZero();
    }

    @Test
    void respostaDeRespostaDesceUmNivel() {
        ReplyResponse raiz = service.reply(
                "beto", reviewDaAna.getId(), new CreateReplyRequest("discordo", null));

        ReplyResponse filha = service.reply(
                "carla", reviewDaAna.getId(), new CreateReplyRequest("por que?", raiz.id()));

        assertThat(filha.depth()).isEqualTo(1);
    }

    @Test
    void aProfundidadeParaNoTeto() {
        // Alem do nivel 3 a coluna fica estreita demais pra caber texto. A quarta
        // resposta encadeada continua no nivel 3, como IRMA da que ela responde -
        // a conversa segue, a indentacao para.
        Long paiId = null;
        int ultimoNivel = -1;

        for (int i = 0; i < 6; i++) {
            ReplyResponse atual = service.reply(
                    "beto", reviewDaAna.getId(), new CreateReplyRequest("nivel " + i, paiId));
            paiId = atual.id();
            ultimoNivel = atual.depth();
        }

        assertThat(ultimoNivel).isEqualTo(ReviewReply.MAX_DEPTH);
    }

    @Test
    void apagarRespostaSemFilhasRemoveDeVerdade() {
        ReplyResponse sozinha = service.reply(
                "beto", reviewDaAna.getId(), new CreateReplyRequest("me arrependi", null));

        service.deleteReply("beto", sozinha.id());

        assertThat(reviewReplyRepository.findById(sozinha.id())).isEmpty();
    }

    @Test
    void apagarRespostaComFilhasViraLapideEPreservaOGalho() {
        ReplyResponse pai = service.reply(
                "beto", reviewDaAna.getId(), new CreateReplyRequest("me arrependi", null));
        ReplyResponse filha = service.reply(
                "carla", reviewDaAna.getId(), new CreateReplyRequest("mas voce tinha razao", pai.id()));

        service.deleteReply("beto", pai.id());

        // O pai continua na arvore, sem texto; a filha continua com o dela.
        assertThat(reviewReplyRepository.findById(pai.id())).isPresent();
        assertThat(reviewReplyRepository.findById(filha.id()))
                .get()
                .extracting(ReviewReply::getText)
                .isEqualTo("mas voce tinha razao");
    }

    @Test
    void soOAutorApagaAPropriaResposta() {
        ReplyResponse doBeto = service.reply(
                "beto", reviewDaAna.getId(), new CreateReplyRequest("minha resposta", null));

        assertThatThrownBy(() -> service.deleteReply("carla", doBeto.id()))
                .isInstanceOf(BadRequestException.class);
    }

    // ---------- montagem da arvore ----------

    @Test
    void montaAArvoreDeRespostasAninhadas() {
        ReplyResponse raiz = service.reply(
                "beto", reviewDaAna.getId(), new CreateReplyRequest("discordo", null));
        service.reply("carla", reviewDaAna.getId(), new CreateReplyRequest("por que?", raiz.id()));
        service.reply("carla", reviewDaAna.getId(), new CreateReplyRequest("outra raiz", null));

        Map<Long, ReviewSocial> social =
                service.loadFor(List.of(reviewDaAna.getId()), null);
        List<ReplyResponse> respostas = social.get(reviewDaAna.getId()).replies();

        // Duas raizes no topo; a primeira com uma filha pendurada.
        assertThat(respostas).hasSize(2);
        assertThat(respostas.get(0).children()).hasSize(1);
        assertThat(respostas.get(0).children().get(0).text()).isEqualTo("por que?");
        assertThat(respostas.get(1).children()).isEmpty();
    }

    @Test
    void aLapideAparecwSemTextoMasComAsFilhas() {
        ReplyResponse pai = service.reply(
                "beto", reviewDaAna.getId(), new CreateReplyRequest("apagavel", null));
        service.reply("carla", reviewDaAna.getId(), new CreateReplyRequest("resposta a ela", pai.id()));
        service.deleteReply("beto", pai.id());

        List<ReplyResponse> respostas = service.loadFor(List.of(reviewDaAna.getId()), null)
                .get(reviewDaAna.getId()).replies();

        assertThat(respostas).hasSize(1);
        assertThat(respostas.get(0).deleted()).isTrue();
        assertThat(respostas.get(0).text()).isNull();
        assertThat(respostas.get(0).children()).hasSize(1);
    }

    @Test
    void loadForDizDeQueLadoOVisitanteVotou() {
        service.vote("beto", reviewDaAna.getId(), VoteType.NEGATIVE);

        Map<Long, ReviewSocial> paraOBeto = service.loadFor(List.of(reviewDaAna.getId()), "beto");
        Map<Long, ReviewSocial> paraACarla = service.loadFor(List.of(reviewDaAna.getId()), "carla");

        assertThat(paraOBeto.get(reviewDaAna.getId()).myVote()).isEqualTo("NEGATIVE");
        // Quem nao votou - e quem nem esta logado - nao tem lado.
        assertThat(paraACarla.get(reviewDaAna.getId()).myVote()).isNull();
    }

    @Test
    void loadForDevolveEntradaVaziaParaAvaliacaoSemNada() {
        // A tela pede o social de todas as avaliacoes da pagina. Uma avaliacao sem
        // voto nem resposta nao pode sumir do mapa, senao o card quebra tentando
        // ler contagem de um nulo.
        Map<Long, ReviewSocial> social = service.loadFor(List.of(reviewDaAna.getId()), null);

        assertThat(social).containsKey(reviewDaAna.getId());
        assertThat(social.get(reviewDaAna.getId()).positiveVotes()).isZero();
        assertThat(social.get(reviewDaAna.getId()).replies()).isEmpty();
    }

    @Test
    void loadForComListaVaziaNaoVaiAoBanco() {
        assertThat(service.loadFor(List.of(), null)).isEmpty();
    }
}
