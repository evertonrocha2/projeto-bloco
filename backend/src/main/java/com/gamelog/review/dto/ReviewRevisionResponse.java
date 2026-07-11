package com.gamelog.review.dto;

import com.gamelog.review.domain.Review;
import com.gamelog.shared.audit.AuditRevision;
import java.time.Instant;
import org.springframework.data.history.Revision;

// Uma "foto" da review em um ponto do passado, com os metadados da revisao:
// numero, tipo (INSERT/UPDATE/DELETE), quando aconteceu e quem fez.
public record ReviewRevisionResponse(
        Long revision,
        String type,
        Instant modifiedAt,
        String modifiedBy,
        int rating,
        String text
) {
    public static ReviewRevisionResponse from(Revision<Long, Review> revision) {
        // O delegate e a nossa AuditRevision - e dela que vem o username.
        AuditRevision meta = (AuditRevision) revision.getMetadata().getDelegate();
        Review snapshot = revision.getEntity();
        return new ReviewRevisionResponse(
                revision.getRequiredRevisionNumber(),
                revision.getMetadata().getRevisionType().name(),
                meta.getInstant(),
                meta.getUsername(),
                snapshot.getRating(),
                snapshot.getText()
        );
    }
}
