package com.gamelog.recommendation.messaging;

import com.gamelog.recommendation.messaging.SnapshotMessages.CatalogSnapshot;
import com.gamelog.recommendation.messaging.SnapshotMessages.SnapshotRequest;
import com.gamelog.recommendation.projection.ActivityProjector;
import com.gamelog.recommendation.projection.ProjectionCheckpoint;
import com.gamelog.recommendation.projection.ProjectionCheckpointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Pede ao monolito o estado inicial, na primeira vez que este servico sobe.
//
// === Request/reply sobre mensageria ===
//
// convertSendAndReceive publica o pedido com replyTo e correlationId e BLOQUEIA ate
// a resposta chegar (ou o timeout vencer). Por baixo, o Spring AMQP usa o "direct
// reply-to" do RabbitMQ: a resposta volta por um canal exclusivo desta conexao,
// sem fila temporaria pra declarar.
//
// E sincrono do ponto de vista de quem chama, mas desacoplado no resto: este
// servico nao sabe o endereco do monolito, e se o monolito estiver fora o pedido
// simplesmente nao e respondido - sem connection refused, sem circuit breaker. A
// tentativa se repete a cada 15s ate dar certo; depois disso, os eventos mantem a
// projecao em dia e o snapshot nao e mais pedido.
@Component
@ConditionalOnProperty(name = "app.snapshot.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class SnapshotBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(SnapshotBootstrapper.class);

    private final RabbitTemplate rabbitTemplate;
    private final ActivityProjector projector;
    private final ProjectionCheckpointRepository checkpointRepository;
    private final RecalculationRequester recalculationRequester;

    public SnapshotBootstrapper(RabbitTemplate rabbitTemplate,
                                ActivityProjector projector,
                                ProjectionCheckpointRepository checkpointRepository,
                                RecalculationRequester recalculationRequester) {
        this.rabbitTemplate = rabbitTemplate;
        this.projector = projector;
        this.checkpointRepository = checkpointRepository;
        this.recalculationRequester = recalculationRequester;
    }

    @Scheduled(initialDelayString = "${app.snapshot.bootstrap.initial-delay-ms:3000}",
            fixedDelayString = "${app.snapshot.bootstrap.retry-interval-ms:15000}")
    public void bootstrapIfNeeded() {
        if (checkpointRepository.existsById(ProjectionCheckpoint.SNAPSHOT)) {
            return;
        }
        requestSnapshot();
    }

    // Tambem chamado pelo endpoint interno de ressincronizacao.
    public boolean requestSnapshot() {
        try {
            CatalogSnapshot snapshot = rabbitTemplate.convertSendAndReceiveAsType(
                    "", Messaging.SNAPSHOT_REQUEST_QUEUE,
                    new SnapshotRequest("recommendation-service"),
                    new ParameterizedTypeReference<CatalogSnapshot>() {
                    });

            if (snapshot == null) {
                log.warn("Monolito nao respondeu o pedido de snapshot; nova tentativa em instantes");
                return false;
            }

            projector.applySnapshot(snapshot);
            // Os lotes gravados antes do snapshot foram calculados com a projecao
            // incompleta. Agora que ela esta completa, vale recalcular todos.
            recalculationRequester.requestForEveryone("snapshot");
            return true;
        } catch (AmqpException e) {
            log.warn("Nao foi possivel pedir o snapshot ao monolito: {}", e.getMessage());
            return false;
        }
    }
}
