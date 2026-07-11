package com.gamelog.shared.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

// Cada transacao que altera uma entidade auditada gera UMA revisao - uma linha
// nessa tabela. As tabelas *_aud apontam pra ca pelo numero da revisao (rev).
//
// A gente customiza a revisao padrao do Envers pra guardar tambem QUEM fez a
// mudanca (username), preenchido pelo AuditRevisionListener. Isso transforma o
// historico em uma trilha de auditoria de verdade: o que mudou, quando e por quem.
@Entity
@Table(name = "audit_revisions")
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private Long id;

    // Envers preenche com o momento do commit da transacao.
    @RevisionTimestamp
    private long timestamp;

    // Quem estava autenticado quando a mudanca aconteceu.
    private String username;

    public Long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Instant getInstant() {
        return Instant.ofEpochMilli(timestamp);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
