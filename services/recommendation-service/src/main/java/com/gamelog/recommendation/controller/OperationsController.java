package com.gamelog.recommendation.controller;

import com.gamelog.recommendation.messaging.Messaging;
import com.gamelog.recommendation.messaging.SnapshotBootstrapper;
import com.gamelog.recommendation.projection.CatalogGameViewRepository;
import com.gamelog.recommendation.projection.ProcessedEventRepository;
import com.gamelog.recommendation.projection.ProjectionCheckpoint;
import com.gamelog.recommendation.projection.ProjectionCheckpointRepository;
import com.gamelog.recommendation.projection.UserCollectionViewRepository;
import com.gamelog.recommendation.projection.UserRatingViewRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Endpoints de OPERACAO da mensageria - pra quem mantem o sistema, nao pra tela.
//
// Ficam em /internal, fora de /api/recommendations/**: o gateway so roteia /api, entao
// estas rotas nao sao alcancaveis de fora. Acessa-se direto no servico (porta 8081,
// ou kubectl port-forward no cluster).
//
//  GET  /internal/messaging              - estado da projecao e profundidade das filas
//  POST /internal/messaging/resync       - descarta o checkpoint e pede snapshot novo
//  POST /internal/messaging/dlq/{fila}/replay - devolve mensagens da DLQ pra fila original
//
// O replay e o fechamento do ciclo da DLQ: depois de corrigir o bug que fazia a
// mensagem falhar (ou de o banco voltar), as mensagens "estacionadas" sao
// reprocessadas em vez de perdidas.
@RestController
@RequestMapping("/internal/messaging")
public class OperationsController {

    private static final Map<String, String[]> DEAD_LETTER_QUEUES = Map.of(
            "activity", new String[]{Messaging.ACTIVITY_DLQ, Messaging.ACTIVITY_QUEUE},
            "recalculate", new String[]{Messaging.RECALCULATE_DLQ, Messaging.RECALCULATE_QUEUE});

    private final CatalogGameViewRepository catalogRepository;
    private final UserRatingViewRepository ratingRepository;
    private final UserCollectionViewRepository collectionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ProjectionCheckpointRepository checkpointRepository;
    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectProvider<SnapshotBootstrapper> bootstrapper;

    public OperationsController(CatalogGameViewRepository catalogRepository,
                                UserRatingViewRepository ratingRepository,
                                UserCollectionViewRepository collectionRepository,
                                ProcessedEventRepository processedEventRepository,
                                ProjectionCheckpointRepository checkpointRepository,
                                AmqpAdmin amqpAdmin,
                                RabbitTemplate rabbitTemplate,
                                ObjectProvider<SnapshotBootstrapper> bootstrapper) {
        this.catalogRepository = catalogRepository;
        this.ratingRepository = ratingRepository;
        this.collectionRepository = collectionRepository;
        this.processedEventRepository = processedEventRepository;
        this.checkpointRepository = checkpointRepository;
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.bootstrapper = bootstrapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String, Object> status() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("snapshotAppliedAt", checkpointRepository.findById(ProjectionCheckpoint.SNAPSHOT)
                .map(ProjectionCheckpoint::getReachedAt).orElse(null));
        projection.put("catalogGames", catalogRepository.count());
        projection.put("ratings", ratingRepository.count());
        projection.put("collectionEntries", collectionRepository.count());
        projection.put("processedEvents", processedEventRepository.count());

        Map<String, Object> queues = new LinkedHashMap<>();
        for (String queue : new String[]{Messaging.ACTIVITY_QUEUE, Messaging.RECALCULATE_QUEUE,
                Messaging.ACTIVITY_DLQ, Messaging.RECALCULATE_DLQ}) {
            QueueInformation info = amqpAdmin.getQueueInfo(queue);
            queues.put(queue, info == null ? null : Map.of(
                    "messages", info.getMessageCount(), "consumers", info.getConsumerCount()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projection", projection);
        body.put("queues", queues);
        return body;
    }

    @PostMapping("/resync")
    public ResponseEntity<Map<String, Object>> resync() {
        checkpointRepository.deleteById(ProjectionCheckpoint.SNAPSHOT);
        boolean applied = Optional.ofNullable(bootstrapper.getIfAvailable())
                .map(SnapshotBootstrapper::requestSnapshot)
                .orElse(false);
        return ResponseEntity.ok(Map.of("snapshotApplied", applied));
    }

    @PostMapping("/dlq/{name}/replay")
    public ResponseEntity<Map<String, Object>> replay(@PathVariable String name,
                                                      @RequestParam(defaultValue = "100") int max) {
        String[] queues = DEAD_LETTER_QUEUES.get(name);
        if (queues == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "fila desconhecida: " + name));
        }

        int moved = 0;
        Message message;
        while (moved < max && (message = rabbitTemplate.receive(queues[0])) != null) {
            // Exchange padrao ("") com o nome da fila como routing key: entrega
            // direto na fila original, sem passar de novo pelo topic. Assim o replay
            // nao duplica a mensagem pros outros assinantes do evento.
            rabbitTemplate.send("", queues[1], message);
            moved++;
        }
        return ResponseEntity.ok(Map.of("replayed", moved, "from", queues[0], "to", queues[1]));
    }
}
