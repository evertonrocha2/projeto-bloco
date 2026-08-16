package com.gamelog.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Superclasse comum de todas as entidades. @MappedSuperclass diz pro JPA que
// ela nao vira tabela propria: as colunas created_at e updated_at aparecem
// direto na tabela de cada entidade filha.
//
// Antes cada entidade setava createdAt na mao com Instant.now(). Agora o
// AuditingEntityListener preenche os dois campos sozinho: @CreatedDate no
// primeiro save e @LastModifiedDate em todo update. Menos codigo repetido e
// zero chance de alguem esquecer de atualizar a data.
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    // updatable = false: depois de criado, ninguem mexe na data de criacao.
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
