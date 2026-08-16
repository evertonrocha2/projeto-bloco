package com.gamelog.review.domain;

import com.gamelog.catalog.domain.Game;
import com.gamelog.identity.domain.User;
import com.gamelog.shared.persistence.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

// Review e o coracao do app: liga um usuario a um jogo, com uma nota e um texto.
// A constraint de unicidade (user_id, game_id) garante que cada pessoa so
// escreve uma review por jogo - isso e uma regra de negocio do dominio.
//
// @Audited: toda mudanca (criar, editar, apagar) vira uma linha na tabela
// reviews_aud, ligada a uma revisao (quem mudou e quando). E assim que o
// historico de reviews funciona.
// @AuditOverride inclui tambem os campos herdados de Auditable no historico.
@Entity
@Audited
@AuditOverride(forClass = Auditable.class)
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_id"}),
        // A pagina de um jogo lista as reviews dele (where game_id = ?).
        // A constraint acima ja indexa user_id; esse indice cobre o outro lado.
        indexes = @Index(name = "idx_reviews_game_id", columnList = "game_id")
)
public class Review extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ManyToOne: muitas reviews apontam pro mesmo usuario. LAZY pra so carregar
    // o usuario do banco quando a gente realmente acessar ele.
    // NOT_AUDITED: o historico guarda so o user_id, sem versionar o User junto
    // (o modulo identity nao e auditado - isolamento entre dominios).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Game game;

    // Nota de 0 a 5. A validacao do intervalo acontece no service.
    @Column(nullable = false)
    private int rating;

    @Column(length = 2000)
    private String text;

    protected Review() {
    }

    public Review(User user, Game game, int rating, String text) {
        this.user = user;
        this.game = game;
        this.rating = rating;
        this.text = text;
    }

    // Editar uma review muda so a opiniao (nota e texto), nunca o autor nem o
    // jogo. Expor um metodo de dominio em vez de setters soltos deixa isso claro.
    public void update(int rating, String text) {
        this.rating = rating;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Game getGame() {
        return game;
    }

    public int getRating() {
        return rating;
    }

    public String getText() {
        return text;
    }
}
