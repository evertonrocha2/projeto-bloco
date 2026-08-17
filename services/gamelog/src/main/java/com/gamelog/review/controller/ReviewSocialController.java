package com.gamelog.review.controller;

import com.gamelog.review.dto.CreateReplyRequest;
import com.gamelog.review.dto.ReplyResponse;
import com.gamelog.review.dto.ReviewSocial;
import com.gamelog.review.dto.VoteRequest;
import com.gamelog.review.service.ReviewSocialService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// A conversa em volta de uma avaliacao: votar e responder.
//
// Separado do ReviewController porque sao coisas de donos diferentes. Aquele
// cuida da avaliacao do PROPRIO usuario (criar, editar, apagar a minha); este
// cuida do que os OUTROS fazem com ela. Todas as rotas exigem login - o
// SecurityConfig so deixa passar quem tem token, e o Principal diz quem e.
@RestController
public class ReviewSocialController {

    private final ReviewSocialService reviewSocialService;

    public ReviewSocialController(ReviewSocialService reviewSocialService) {
        this.reviewSocialService = reviewSocialService;
    }

    // PUT e nao POST: da tela e UM gesto - clicar no polegar - e o resultado e o
    // mesmo independente de quantas vezes a requisicao chegue com o mesmo corpo
    // partindo do mesmo estado. Vota, troca de lado ou desfaz, conforme o que ja
    // existe; o service decide.
    //
    // Devolve o placar ja recalculado pra tela pintar sem uma segunda chamada.
    @PutMapping("/api/reviews/{reviewId}/vote")
    public ReviewSocial vote(
            @PathVariable Long reviewId,
            @Valid @RequestBody VoteRequest request,
            Principal principal
    ) {
        return reviewSocialService.vote(principal.getName(), reviewId, request.type());
    }

    // Remove o voto sem precisar saber qual era. Idempotente: sem voto nenhum,
    // tambem responde 204.
    @DeleteMapping("/api/reviews/{reviewId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVote(@PathVariable Long reviewId, Principal principal) {
        reviewSocialService.removeVote(principal.getName(), reviewId);
    }

    @PostMapping("/api/reviews/{reviewId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ReplyResponse reply(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReplyRequest request,
            Principal principal
    ) {
        return reviewSocialService.reply(principal.getName(), reviewId, request);
    }

    // A rota nao passa pelo id da avaliacao: uma resposta ja sabe a qual conversa
    // pertence, e exigir os dois abriria a porta pra combinacoes incoerentes.
    @DeleteMapping("/api/replies/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReply(@PathVariable Long replyId, Principal principal) {
        reviewSocialService.deleteReply(principal.getName(), replyId);
    }
}
