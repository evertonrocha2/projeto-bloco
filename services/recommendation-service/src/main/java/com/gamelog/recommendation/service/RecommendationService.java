package com.gamelog.recommendation.service;

import com.gamelog.recommendation.client.ActivitySource;
import com.gamelog.recommendation.client.GameLogSnapshot;
import com.gamelog.recommendation.config.ScoringProperties;
import com.gamelog.recommendation.domain.FeedbackEntry;
import com.gamelog.recommendation.domain.FeedbackVerdict;
import com.gamelog.recommendation.domain.Recommendation;
import com.gamelog.recommendation.domain.RecommendationEngine;
import com.gamelog.recommendation.domain.RecommendationFeedback;
import com.gamelog.recommendation.domain.ScoredGame;
import com.gamelog.recommendation.domain.TasteProfile;
import com.gamelog.recommendation.dto.RecommendationItem;
import com.gamelog.recommendation.dto.RecommendationsResponse;
import com.gamelog.recommendation.dto.TasteProfileResponse;
import com.gamelog.recommendation.repository.RecommendationFeedbackRepository;
import com.gamelog.recommendation.repository.RecommendationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Orquestra o microsservico: busca o retrato do GameLog, roda o algoritmo, grava
// o lote e registra feedback.
//
// Repare no que ele NAO faz: nao sabe o que e Feign, nem Eureka, nem circuit
// breaker. Ele depende do ActivitySource, e o problema de "como falar com o outro
// servico" fica inteiro do outro lado dessa interface.
@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationFeedbackRepository feedbackRepository;
    private final RecommendationEngine engine;
    private final ActivitySource activitySource;
    private final ScoringProperties scoringProperties;

    public RecommendationService(RecommendationRepository recommendationRepository,
                                RecommendationFeedbackRepository feedbackRepository,
                                RecommendationEngine engine,
                                ActivitySource activitySource,
                                ScoringProperties scoringProperties) {
        this.recommendationRepository = recommendationRepository;
        this.feedbackRepository = feedbackRepository;
        this.engine = engine;
        this.activitySource = activitySource;
        this.scoringProperties = scoringProperties;
    }

    // Serve as recomendacoes vigentes. Se o usuario ainda nao tem lote, gera um.
    //
    // Ler do banco em vez de recalcular a cada abertura de tela e uma escolha de
    // desempenho e de acoplamento: abrir a tela nao pode custar duas chamadas de
    // rede ao monolito. Recalcular passa a ser uma acao explicita do usuario
    // (botao "recalcular") ou o caso do primeiro acesso.
    @Transactional
    public RecommendationsResponse getRecommendations(String username) {
        List<Recommendation> stored =
                recommendationRepository.findByUsernameOrderByScoreDesc(username);

        if (!stored.isEmpty()) {
            return toResponse(username, stored, false);
        }

        return refresh(username);
    }

    // Recalcula do zero: busca atividade + catalogo no monolito, pontua e
    // substitui o lote gravado.
    @Transactional
    public RecommendationsResponse refresh(String username) {
        Optional<GameLogSnapshot> snapshot = activitySource.fetch(username);

        // === Caminho degradado ===
        // Nao deu pra falar com o monolito (fora do ar, lento ou disjuntor aberto).
        // Em vez de propagar o erro, devolve o ultimo lote gravado marcado como
        // desatualizado. E aqui que o banco proprio do microsservico deixa de ser
        // detalhe de implementacao e passa a ser o que mantem a feature de pe.
        if (snapshot.isEmpty()) {
            return toResponse(username,
                    recommendationRepository.findByUsernameOrderByScoreDesc(username),
                    true);
        }

        List<FeedbackEntry> feedback = feedbackEntriesOf(username);

        List<ScoredGame> scored = engine.recommend(
                snapshot.get().activity(),
                snapshot.get().catalog(),
                feedback,
                // Lido a cada chamada, e nao guardado em campo: e o que faz um
                // /actuator/refresh valer na proxima requisicao.
                scoringProperties.toWeights());

        Instant generatedAt = Instant.now();

        // Substituir, nao acrescentar: sem o delete o mesmo jogo apareceria
        // repetido e a constraint (username, game_id) estouraria.
        recommendationRepository.deleteByUsername(username);
        List<Recommendation> saved = recommendationRepository.saveAll(
                scored.stream()
                        .map(game -> Recommendation.from(username, game, generatedAt))
                        .toList());

        return toResponse(username, saved, false);
    }

    // Registra o que o usuario achou de um jogo recomendado.
    @Transactional
    public void registerFeedback(String username, Long gameId, FeedbackVerdict verdict) {
        // Atualiza o veredito existente em vez de acumular opinioes: guardar as duas
        // daria ao algoritmo sinais contraditorios sobre o mesmo jogo.
        RecommendationFeedback feedback = feedbackRepository
                .findByUsernameAndGameId(username, gameId)
                .orElseGet(() -> new RecommendationFeedback(username, gameId, verdict));
        feedback.setVerdict(verdict);
        feedbackRepository.save(feedback);

        // Tira o jogo do lote atual. Vale pros dois vereditos: o usuario acabou de
        // reagir aquele card, ele nao deve continuar na lista. Sem isso, um jogo
        // descartado ficaria na tela ate o proximo recalculo.
        recommendationRepository.deleteByUsernameAndGameId(username, gameId);
    }

    // Expoe o perfil de gosto calculado - o "porque" das recomendacoes.
    @Transactional(readOnly = true)
    public TasteProfileResponse getTasteProfile(String username) {
        Optional<GameLogSnapshot> snapshot = activitySource.fetch(username);

        // Sem o monolito nao ha como calcular perfil: ele depende das avaliacoes,
        // que sao dado do outro servico. Perfil vazio e a resposta honesta.
        if (snapshot.isEmpty()) {
            return new TasteProfileResponse(username, List.of());
        }

        TasteProfile profile = TasteProfile.from(
                snapshot.get().activity(),
                snapshot.get().catalog(),
                feedbackEntriesOf(username),
                scoringProperties.toWeights());

        List<TasteProfileResponse.GenreWeight> genres = profile.rankedGenres().stream()
                .map(entry -> new TasteProfileResponse.GenreWeight(
                        entry.getKey(), round(entry.getValue())))
                .toList();

        return new TasteProfileResponse(username, genres);
    }

    private List<FeedbackEntry> feedbackEntriesOf(String username) {
        return feedbackRepository.findByUsername(username).stream()
                .map(RecommendationFeedback::toEntry)
                .toList();
    }

    private RecommendationsResponse toResponse(String username,
                                               List<Recommendation> recommendations,
                                               boolean stale) {
        // generatedAt nulo quando nao existe lote nenhum - a tela distingue "nunca
        // calculei" de "calculei e nao achei nada".
        Instant generatedAt = recommendations.isEmpty()
                ? null
                : recommendations.get(0).getGeneratedAt();

        return new RecommendationsResponse(
                username,
                generatedAt,
                stale,
                recommendations.stream().map(RecommendationItem::from).toList());
    }

    // O peso vai pra tela como largura de barra; duas casas bastam e evitam
    // 0.6000000000000001 no JSON.
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
