package com.gamelog.review.service;

import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.review.domain.ReviewReply;
import com.gamelog.review.domain.ReviewVote;
import com.gamelog.review.domain.VoteType;
import com.gamelog.review.dto.CreateReplyRequest;
import com.gamelog.review.dto.ReplyResponse;
import com.gamelog.review.dto.ReviewSocial;
import com.gamelog.review.repository.ReviewReplyRepository;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.review.repository.ReviewVoteRepository;
import com.gamelog.shared.BadRequestException;
import com.gamelog.shared.NotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// A camada social das avaliacoes: votar e responder.
//
// Separado do ReviewService de proposito. Aquele ja concentra criar, editar,
// apagar, listar, calcular medias e historico; empilhar voto e arvore de resposta
// ali faria um arquivo que ninguem le inteiro. Sao dois assuntos: um e "a minha
// opiniao sobre o jogo", o outro e "a conversa em volta da opiniao de alguem".
@Service
public class ReviewSocialService {

    private final ReviewVoteRepository voteRepository;
    private final ReviewReplyRepository replyRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewSocialService(ReviewVoteRepository voteRepository,
                               ReviewReplyRepository replyRepository,
                               ReviewRepository reviewRepository,
                               UserRepository userRepository) {
        this.voteRepository = voteRepository;
        this.replyRepository = replyRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    // ---------- votos ----------

    // Vota, troca de lado ou desfaz - dependendo do que ja existe.
    //
    // Tres comportamentos numa rota so porque, da tela, e UM gesto: clicar no
    // polegar. Exigir que o front descubra qual das tres operacoes fazer seria
    // devolver pra ele o estado que so o servidor conhece com certeza.
    @Transactional
    public ReviewSocial vote(String username, Long reviewId, VoteType type) {
        User user = findUser(username);
        Review review = findReview(reviewId);

        // Votar em si mesmo transformaria o placar numa medida de auto-elogio.
        if (review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Nao da pra votar na propria avaliacao");
        }

        Optional<ReviewVote> existente = voteRepository.findByUserIdAndReviewId(user.getId(), reviewId);

        if (existente.isPresent()) {
            ReviewVote voto = existente.get();

            if (voto.getType() == type) {
                // Mesmo lado de novo: e o clique que desmarca.
                voteRepository.delete(voto);
            } else {
                voto.changeTo(type);
                voteRepository.save(voto);
            }
        } else {
            voteRepository.save(new ReviewVote(user, review, type));
        }

        // Devolve o estado ja recalculado: a tela pinta o resultado sem uma
        // segunda requisicao.
        return loadFor(List.of(reviewId), username).get(reviewId);
    }

    // Remove o voto, sem precisar saber qual era. Idempotente: sem voto nenhum,
    // tambem termina em silencio - dois cliques rapidos nao viram erro na cara da
    // pessoa.
    @Transactional
    public void removeVote(String username, Long reviewId) {
        User user = findUser(username);
        voteRepository.findByUserIdAndReviewId(user.getId(), reviewId)
                .ifPresent(voteRepository::delete);
    }

    // ---------- respostas ----------

    @Transactional
    public ReplyResponse reply(String username, Long reviewId, CreateReplyRequest request) {
        User user = findUser(username);
        Review review = findReview(reviewId);

        ReviewReply parent = null;
        if (request.parentId() != null) {
            parent = replyRepository.findById(request.parentId())
                    .orElseThrow(() -> new NotFoundException("Resposta nao encontrada"));

            // Responder uma resposta de OUTRA avaliacao juntaria duas conversas
            // que nao se conhecem.
            if (!parent.getReview().getId().equals(reviewId)) {
                throw new BadRequestException("Essa resposta e de outra avaliacao");
            }
        }

        // O teto de profundidade e aplicado no construtor da entidade.
        ReviewReply reply = replyRepository.save(new ReviewReply(review, user, parent, request.text()));
        return ReplyResponse.from(reply, List.of());
    }

    // Apagar a propria resposta.
    //
    // Com filhas vira lapide; sem filhas sai da tabela. A diferenca importa: uma
    // resposta com filhas e um no da arvore, e remover o no levaria tudo o que
    // pende dele - texto de outras pessoas, que nao pediram pra desaparecer.
    @Transactional
    public void deleteReply(String username, Long replyId) {
        ReviewReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new NotFoundException("Resposta nao encontrada"));

        if (!reply.getUser().getUsername().equals(username)) {
            throw new BadRequestException("Essa resposta nao e sua");
        }

        if (replyRepository.existsByParentId(replyId)) {
            reply.tombstone();
            replyRepository.save(reply);
        } else {
            replyRepository.delete(reply);
        }
    }

    // ---------- leitura em lote ----------

    // O social de VARIAS avaliacoes de uma vez: contagens, o voto de quem esta
    // olhando, e a arvore de respostas montada.
    //
    // Em lote porque a pagina de um jogo lista N avaliacoes. Fazer isso por
    // avaliacao seria 3N consultas pra desenhar uma tela, e o numero cresce com a
    // popularidade do jogo - a pagina ficaria mais lenta exatamente onde mais
    // gente entra. Aqui sao tres consultas, independente de N.
    //
    // viewerUsername pode ser nulo: a pagina de um jogo e publica, e quem nao
    // esta logado ve as contagens sem ter lado nenhum.
    @Transactional(readOnly = true)
    public Map<Long, ReviewSocial> loadFor(List<Long> reviewIds, String viewerUsername) {
        if (reviewIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, long[]> contagens = contarVotos(reviewIds);
        Map<Long, String> meuVoto = buscarMeuVoto(reviewIds, viewerUsername);
        Map<Long, List<ReplyResponse>> arvores = montarArvores(reviewIds);

        Map<Long, ReviewSocial> resultado = new LinkedHashMap<>();
        for (Long reviewId : reviewIds) {
            long[] placar = contagens.getOrDefault(reviewId, new long[]{0, 0});

            resultado.put(reviewId, new ReviewSocial(
                    placar[0],
                    placar[1],
                    meuVoto.get(reviewId),
                    arvores.getOrDefault(reviewId, List.of())
            ));
        }

        return resultado;
    }

    // Positivos e negativos por avaliacao, no formato [positivos, negativos].
    private Map<Long, long[]> contarVotos(List<Long> reviewIds) {
        Map<Long, long[]> porReview = new HashMap<>();

        for (Object[] linha : voteRepository.countByTypeForReviews(reviewIds)) {
            Long reviewId = (Long) linha[0];
            VoteType tipo = (VoteType) linha[1];
            long total = (Long) linha[2];

            long[] placar = porReview.computeIfAbsent(reviewId, chave -> new long[]{0, 0});
            placar[tipo == VoteType.POSITIVE ? 0 : 1] = total;
        }

        return porReview;
    }

    private Map<Long, String> buscarMeuVoto(List<Long> reviewIds, String viewerUsername) {
        if (viewerUsername == null) {
            return Map.of();
        }

        // Visitante logado que nao existe mais no banco: sem lado, sem erro.
        Optional<User> viewer = userRepository.findByUsername(viewerUsername);
        if (viewer.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> porReview = new HashMap<>();
        for (ReviewVote voto : voteRepository.findByUserIdAndReviewIdIn(viewer.get().getId(), reviewIds)) {
            porReview.put(voto.getReview().getId(), voto.getType().name());
        }

        return porReview;
    }

    // Monta a arvore de respostas de cada avaliacao a partir de UMA consulta
    // plana.
    //
    // O algoritmo e de uma passada: agrupa as respostas por pai, depois desce
    // recursivamente a partir das raizes. Alternativa seria consultar filho por
    // filho, que e uma ida ao banco por no - trinta respostas, trinta consultas.
    //
    // A ordem cronologica vem da consulta, entao irmaos saem na ordem em que
    // foram escritos sem nenhuma reordenacao aqui.
    private Map<Long, List<ReplyResponse>> montarArvores(List<Long> reviewIds) {
        List<ReviewReply> todas = replyRepository.findByReviewIdInOrderByCreatedAtAsc(reviewIds);

        // pai -> filhas. A chave null junta as raizes de cada avaliacao, por isso
        // as raizes ficam separadas por reviewId num mapa proprio.
        Map<Long, List<ReviewReply>> filhasPorPai = new HashMap<>();
        Map<Long, List<ReviewReply>> raizesPorReview = new LinkedHashMap<>();

        for (ReviewReply reply : todas) {
            if (reply.getParent() == null) {
                raizesPorReview
                        .computeIfAbsent(reply.getReview().getId(), chave -> new ArrayList<>())
                        .add(reply);
            } else {
                filhasPorPai
                        .computeIfAbsent(reply.getParent().getId(), chave -> new ArrayList<>())
                        .add(reply);
            }
        }

        Map<Long, List<ReplyResponse>> arvores = new LinkedHashMap<>();
        raizesPorReview.forEach((reviewId, raizes) ->
                arvores.put(reviewId, raizes.stream()
                        .map(raiz -> descer(raiz, filhasPorPai))
                        .toList()));

        return arvores;
    }

    // Transforma uma resposta e tudo o que pende dela num ReplyResponse.
    //
    // A recursao e limitada pelo teto de profundidade da entidade (MAX_DEPTH),
    // entao nao ha risco de pilha estourar por conversa funda.
    private ReplyResponse descer(ReviewReply reply, Map<Long, List<ReviewReply>> filhasPorPai) {
        List<ReplyResponse> filhas = filhasPorPai.getOrDefault(reply.getId(), List.of()).stream()
                .map(filha -> descer(filha, filhasPorPai))
                .toList();

        return ReplyResponse.from(reply, filhas);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));
    }

    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review nao encontrada"));
    }
}
