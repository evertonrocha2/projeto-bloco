package com.gamelog.recommendation.messaging;

import com.gamelog.recommendation.projection.ProjectionOutcome;
import com.gamelog.recommendation.repository.RecommendationRepository;
import java.time.Clock;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// Decide QUEM precisa ter as recomendacoes recalculadas depois de um evento, e
// manda um comando pra cada um.
//
// So vale pra quem ja tem um lote gravado. Quem nunca abriu a tela de
// recomendacoes nao precisa de recalculo: o primeiro acesso ja calcula na hora. Sem
// esse filtro, cada review de um usuario que nunca usou a feature custaria um
// recalculo que ninguem vai ver.
@Component
public class RecalculationRequester {

    private static final Logger log = LoggerFactory.getLogger(RecalculationRequester.class);

    private final RabbitTemplate rabbitTemplate;
    private final RecommendationRepository recommendationRepository;
    private final Clock clock;

    public RecalculationRequester(RabbitTemplate rabbitTemplate,
                                  RecommendationRepository recommendationRepository,
                                  Clock clock) {
        this.rabbitTemplate = rabbitTemplate;
        this.recommendationRepository = recommendationRepository;
        this.clock = clock;
    }

    public void requestFor(ProjectionOutcome outcome, GameLogEvent cause) {
        // Jogo novo no catalogo muda os candidatos de TODO mundo. E o caso que
        // mostra por que o recalculo e uma fila de trabalho com consumidores
        // concorrentes: um evento vira N comandos, divididos entre as replicas.
        Collection<String> users = outcome.catalogChanged()
                ? recommendationRepository.findDistinctUsernames()
                : outcome.affectedUsers().stream().filter(recommendationRepository::existsByUsername).toList();

        send(users, cause.eventType(), cause.eventId());
    }

    public void requestForEveryone(String reason) {
        send(recommendationRepository.findDistinctUsernames(), reason, null);
    }

    private void send(Collection<String> users, String reason, String causationId) {
        for (String username : users) {
            rabbitTemplate.convertAndSend(Messaging.COMMANDS_EXCHANGE, Messaging.RECALCULATE_ROUTING_KEY,
                    new RecalculateCommand(username, reason, causationId, clock.instant()));
        }
        if (!users.isEmpty()) {
            log.info("Recalculo pedido para {} usuario(s) por '{}'", users.size(), reason);
        }
    }
}
