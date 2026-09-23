package com.gamelog.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.notification.domain.FeedItem;
import com.gamelog.notification.domain.Notification;
import com.gamelog.notification.domain.NotificationType;
import com.gamelog.notification.domain.ProcessedEvent;
import com.gamelog.notification.messaging.EventPayloads.ReviewPublished;
import com.gamelog.notification.messaging.EventPayloads.ReviewReplied;
import com.gamelog.notification.messaging.EventPayloads.ReviewVoted;
import com.gamelog.notification.messaging.EventPayloads.UserRegistered;
import com.gamelog.notification.messaging.GameLogEvent;
import com.gamelog.notification.repository.FeedItemRepository;
import com.gamelog.notification.repository.NotificationRepository;
import com.gamelog.notification.repository.ProcessedEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Transforma eventos do GameLog em avisos e itens de feed.
//
// As regras de QUEM avisar moram aqui, e nao no monolito. O evento review.replied
// so conta quem esta envolvido (dono da avaliacao, autor da resposta, dono da
// resposta respondida); decidir que ninguem e avisado da propria acao, ou que o
// dono da avaliacao nao recebe dois avisos pela mesma resposta, e politica deste
// servico. Se amanha a regra mudar, muda so aqui.
@Service
public class NotificationProjector {

    static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final NotificationRepository notificationRepository;
    private final FeedItemRepository feedItemRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationProjector(NotificationRepository notificationRepository,
                                 FeedItemRepository feedItemRepository,
                                 ProcessedEventRepository processedEventRepository,
                                 ObjectMapper objectMapper,
                                 Clock clock) {
        this.notificationRepository = notificationRepository;
        this.feedItemRepository = feedItemRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    // Devolve quantos avisos foram criados (0 tambem para duplicata).
    @Transactional
    public int apply(GameLogEvent event) {
        if (event.eventId() == null || event.eventType() == null || event.payload() == null) {
            throw new AmqpRejectAndDontRequeueException("Evento sem eventId/eventType/payload");
        }
        if (event.schemaVersion() > SUPPORTED_SCHEMA_VERSION) {
            throw new AmqpRejectAndDontRequeueException(
                    "Versao " + event.schemaVersion() + " de " + event.eventType() + " nao suportada");
        }
        if (processedEventRepository.existsById(event.eventId())) {
            return 0;
        }

        Instant when = event.occurredAt() != null ? event.occurredAt() : clock.instant();
        List<Notification> created = switch (event.eventType()) {
            case "user.registered" -> welcome(event, read(event, UserRegistered.class), when);
            case "review.replied" -> replied(event, read(event, ReviewReplied.class), when);
            case "review.voted" -> voted(event, read(event, ReviewVoted.class), when);
            case "review.created" -> {
                addToFeed(read(event, ReviewPublished.class), when);
                yield List.of();
            }
            case "review.deleted" -> {
                ReviewPublished review = read(event, ReviewPublished.class);
                if (review.reviewId() != null) {
                    feedItemRepository.deleteById(review.reviewId());
                }
                yield List.of();
            }
            // Evento que casou com a assinatura mas este servico nao trata: ignora
            // e confirma. Nao e erro - o produtor pode ter evoluido.
            default -> List.of();
        };

        notificationRepository.saveAll(created);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), event.eventType(), clock.instant()));
        return created.size();
    }

    private List<Notification> welcome(GameLogEvent event, UserRegistered user, Instant when) {
        return List.of(new Notification(user.username(), NotificationType.WELCOME,
                "Bem-vindo ao GameLog, " + user.username() + "! Avalie alguns jogos para receber recomendacoes.",
                null, null, null, event.eventId(), when));
    }

    private List<Notification> replied(GameLogEvent event, ReviewReplied reply, Instant when) {
        List<Notification> result = new ArrayList<>();
        String actor = reply.replyAuthor();
        String excerpt = reply.excerpt() == null ? "" : ": \"" + reply.excerpt() + "\"";

        if (notSelf(reply.reviewAuthor(), actor)) {
            result.add(new Notification(reply.reviewAuthor(), NotificationType.REVIEW_REPLY,
                    actor + " respondeu sua avaliacao de " + reply.gameTitle() + excerpt,
                    actor, reply.reviewId(), reply.gameId(), event.eventId(), when));
        }
        // O dono da resposta respondida so recebe o aviso de "thread" se ja nao
        // recebeu o de cima (quando ele e tambem o dono da avaliacao).
        if (notSelf(reply.parentAuthor(), actor) && !reply.parentAuthor().equals(reply.reviewAuthor())) {
            result.add(new Notification(reply.parentAuthor(), NotificationType.THREAD_REPLY,
                    actor + " respondeu seu comentario em " + reply.gameTitle() + excerpt,
                    actor, reply.reviewId(), reply.gameId(), event.eventId(), when));
        }
        return result;
    }

    private List<Notification> voted(GameLogEvent event, ReviewVoted vote, Instant when) {
        // So voto positivo vira aviso. Avisar "alguem nao gostou da sua avaliacao"
        // nao ajuda ninguem, e o numero ja aparece na tela do jogo.
        if (!"POSITIVE".equals(vote.voteType()) || !notSelf(vote.reviewAuthor(), vote.voter())) {
            return List.of();
        }
        return List.of(new Notification(vote.reviewAuthor(), NotificationType.REVIEW_VOTE,
                vote.voter() + " achou util sua avaliacao de " + vote.gameTitle(),
                vote.voter(), vote.reviewId(), vote.gameId(), event.eventId(), when));
    }

    private void addToFeed(ReviewPublished review, Instant when) {
        if (review.reviewId() == null || review.gameId() == null) {
            throw new AmqpRejectAndDontRequeueException("review.created sem reviewId/gameId");
        }
        feedItemRepository.save(new FeedItem(review.reviewId(), review.username(), review.gameId(),
                review.gameTitle(), review.rating(), when));
    }

    private static boolean notSelf(String recipient, String actor) {
        return recipient != null && !Objects.equals(recipient, actor);
    }

    private <T> T read(GameLogEvent event, Class<T> type) {
        try {
            return objectMapper.treeToValue(event.payload(), type);
        } catch (Exception e) {
            throw new AmqpRejectAndDontRequeueException(
                    "Payload invalido em " + event.eventType() + " (" + event.eventId() + ")", e);
        }
    }
}
