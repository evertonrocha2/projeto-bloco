package com.gamelog.recommendation.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// Marca de ate onde a projecao esta sincronizada. Hoje ha uma so: "snapshot",
// gravada quando o snapshot inicial do monolito foi aplicado. Sem ela a projecao
// esta incompleta (so tem o que chegou por evento depois que o servico subiu).
@Entity
@Table(name = "projection_checkpoints")
public class ProjectionCheckpoint {

    public static final String SNAPSHOT = "snapshot";

    @Id
    @Column(length = 40)
    private String name;

    @Column(name = "reached_at", nullable = false)
    private Instant reachedAt;

    protected ProjectionCheckpoint() {
    }

    public ProjectionCheckpoint(String name, Instant reachedAt) {
        this.name = name;
        this.reachedAt = reachedAt;
    }

    public String getName() {
        return name;
    }

    public Instant getReachedAt() {
        return reachedAt;
    }
}
