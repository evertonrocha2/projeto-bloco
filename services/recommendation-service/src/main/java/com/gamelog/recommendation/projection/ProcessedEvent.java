package com.gamelog.recommendation.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

// Registro dos eventos ja aplicados - a base do consumidor idempotente.
//
// O monolito entrega "pelo menos uma vez": se ele cair depois de publicar e antes
// de marcar o evento como enviado, o mesmo evento sai de novo. Sem esta tabela, a
// mesma nota seria aplicada duas vezes. Com ela, a segunda entrega e reconhecida
// pelo eventId e ignorada.
//
// A linha e gravada na MESMA transacao que aplica o evento. Se a aplicacao falhar,
// o registro tambem nao fica - e o evento pode ser tentado de novo.
@Entity
@Table(name = "processed_events",
        indexes = @Index(name = "idx_processed_events_at", columnList = "processed_at"))
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
