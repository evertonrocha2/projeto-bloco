package com.gamelog.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

// Uma linha da tabela de outbox: um evento que JA aconteceu e ainda precisa (ou
// precisou) ser entregue ao broker.
//
// === O problema que o outbox resolve (dual write) ===
//
// Publicar direto no RabbitMQ de dentro do ReviewService parece mais simples, mas
// sao duas escritas em dois sistemas sem transacao em comum:
//
//   - salva a review, o broker esta fora do ar -> a review existe e o evento
//     sumiu. O microsservico nunca fica sabendo dela.
//   - publica o evento, o commit falha depois  -> o evento anuncia uma review que
//     nao existe.
//
// Com o outbox, o evento e so mais uma linha gravada NA MESMA transacao do dado.
// Ou os dois entram, ou nenhum entra. A entrega ao broker vira um passo separado
// (OutboxRelay), que pode falhar e tentar de novo sem nunca perder o evento.
@Entity
@Table(name = "outbox_events", indexes = {
        // O relay pergunta, a cada meio segundo, "o que ainda nao foi publicado".
        @Index(name = "idx_outbox_published_at", columnList = "published_at")
})
public class OutboxEvent {

    // Sequencial, e nao o UUID do evento, porque e a ORDEM de gravacao: o relay
    // publica em ordem crescente deste id, e assim "review criada" nunca sai
    // depois de "review apagada" do mesmo agregado.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    // O envelope inteiro, ja serializado. Serializar na hora de gravar (e nao na
    // hora de publicar) congela o evento como ele era no commit: se a entidade
    // mudar antes de o relay rodar, o evento continua contando o fato original.
    @Column(nullable = false, length = 16000)
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    // Nulo enquanto o broker nao confirmou o recebimento.
    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(String eventId, String eventType, String aggregateId,
                       String payload, Instant occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public void markPublished(Instant when) {
        this.publishedAt = when;
        this.attempts++;
        this.lastError = null;
    }

    // A falha fica registrada na propria linha: da pra ver no banco quais eventos
    // estao travados e por que, sem garimpar log.
    public void registerFailure(String error) {
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }
}
