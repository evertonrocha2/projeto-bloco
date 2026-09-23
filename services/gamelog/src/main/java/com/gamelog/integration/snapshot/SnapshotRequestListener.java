package com.gamelog.integration.snapshot;

import com.gamelog.messaging.Messaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Atende pedidos de snapshot que chegam pelo RabbitMQ (padrao request/reply).
//
// O VALOR DE RETORNO do metodo e a resposta: o Spring AMQP le o replyTo da
// mensagem recebida e publica o retorno la, com o mesmo correlationId. Quem pediu
// usa o "direct reply-to" do RabbitMQ (amq.rabbitmq.reply-to), entao nao existe
// fila de resposta pra declarar nem pra limpar.
//
// Comparado ao GET HTTP que o microsservico fazia no TP3, a diferenca pratica e o
// desacoplamento de LOCALIZACAO e de DISPONIBILIDADE: quem pede nao sabe o endereco
// do monolito (nem precisa do Eureka pra isso), e se o monolito estiver fora o
// pedido espera na fila ate o TTL em vez de falhar na conexao.
@Component
public class SnapshotRequestListener {

    private static final Logger log = LoggerFactory.getLogger(SnapshotRequestListener.class);

    private final SnapshotService snapshotService;

    public SnapshotRequestListener(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @RabbitListener(queues = Messaging.SNAPSHOT_REQUEST_QUEUE)
    public CatalogSnapshot onSnapshotRequest(SnapshotRequest request) {
        CatalogSnapshot snapshot = snapshotService.current();
        log.info("Snapshot pedido por '{}': {} jogos, {} notas, {} itens de colecao",
                request.requestedBy(), snapshot.games().size(), snapshot.ratings().size(),
                snapshot.collection().size());
        return snapshot;
    }
}
