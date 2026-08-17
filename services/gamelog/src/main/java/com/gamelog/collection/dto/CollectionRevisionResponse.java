package com.gamelog.collection.dto;

import com.gamelog.collection.domain.CollectionEntry;
import com.gamelog.shared.audit.AuditRevision;
import java.time.Instant;
import org.springframework.data.history.Revision;

// Uma "foto" de um item da colecao em um ponto do passado. Com isso o usuario
// consegue ver a linha do tempo: "adicionei querendo jogar, comecei em marco,
// zerei com 80 horas".
public record CollectionRevisionResponse(
        Long revision,
        String type,
        Instant modifiedAt,
        String modifiedBy,
        int hoursPlayed,
        String status
) {
    public static CollectionRevisionResponse from(Revision<Long, CollectionEntry> revision) {
        AuditRevision meta = (AuditRevision) revision.getMetadata().getDelegate();
        CollectionEntry snapshot = revision.getEntity();
        return new CollectionRevisionResponse(
                revision.getRequiredRevisionNumber(),
                revision.getMetadata().getRevisionType().name(),
                meta.getInstant(),
                meta.getUsername(),
                snapshot.getHoursPlayed(),
                snapshot.getStatus().getLabel()
        );
    }
}
