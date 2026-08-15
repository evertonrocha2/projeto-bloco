package com.gamelog.recommendation.controller;

import com.gamelog.recommendation.dto.FeedbackRequest;
import com.gamelog.recommendation.dto.RecommendationsResponse;
import com.gamelog.recommendation.dto.TasteProfileResponse;
import com.gamelog.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// A API REST do microsservico.
//
// O front nao chama estas rotas direto na porta 8081: ele fala com o gateway na
// 8090, que roteia /api/recommendations/** pra ca. Pra quem usa a aplicacao existe
// um endereco so, e a divisao em servicos fica invisivel.
//
// Mesma divisao de responsabilidade das camadas do monolito: o controller
// transporta, o service decide.
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    // Recomendacoes vigentes. Publico (leitura), igual ao perfil publico do
    // monolito. Gera na hora se for o primeiro acesso do usuario.
    @GetMapping("/{username}")
    public RecommendationsResponse getRecommendations(@PathVariable String username) {
        return recommendationService.getRecommendations(username);
    }

    // Recalcula. Exige token: o gateway barra a requisicao sem Authorization antes
    // de chegar aqui (ver AuthenticationFilter no api-gateway).
    @PostMapping("/{username}/refresh")
    public RecommendationsResponse refresh(@PathVariable String username) {
        return recommendationService.refresh(username);
    }

    // Registra "gostei" ou "nao me interessa". 204: a acao teve efeito e nao ha
    // corpo pra devolver - a tela ja sabe qual card removeu.
    @PostMapping("/{username}/feedback")
    public ResponseEntity<Void> registerFeedback(@PathVariable String username,
                                                 @Valid @RequestBody FeedbackRequest request) {
        recommendationService.registerFeedback(username, request.gameId(), request.verdict());
        return ResponseEntity.noContent().build();
    }

    // O perfil de gosto calculado. E o que permite a tela explicar as
    // recomendacoes em vez de so lista-las.
    @GetMapping("/{username}/taste-profile")
    public TasteProfileResponse getTasteProfile(@PathVariable String username) {
        return recommendationService.getTasteProfile(username);
    }
}
