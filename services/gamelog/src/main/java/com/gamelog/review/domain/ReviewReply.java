package com.gamelog.review.domain;

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
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

// Uma resposta a uma avaliacao, ou a outra resposta.
//
// A conversa e uma arvore: parent aponta pra resposta acima, e null significa
// "pendurada direto na avaliacao". A arvore e montada em memoria pelo service, a
// partir de uma consulta plana - aqui so mora a estrutura.
@Entity
@Audited
@AuditOverride(forClass = Auditable.class)
@Table(
        name = "review_replies",
        // A pagina de um jogo busca as respostas por avaliacao.
        indexes = @Index(name = "idx_review_replies_review_id", columnList = "review_id")
)
public class ReviewReply extends Auditable {

    // Ate onde a indentacao vai. Alem disso a coluna fica estreita demais pra
    // caber texto, e a discussao passa a ser ilegivel justamente onde ficou mais
    // acesa. A resposta que passaria do teto vira irma da que ela responde, e a
    // tela poe um @fulano no comeco pra nao perder a quem se dirigia.
    public static final int MAX_DEPTH = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private User user;

    // Auto-relacionamento. Nulo na resposta pendurada direto na avaliacao.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private ReviewReply parent;

    // Anulavel SO por causa da lapide. Ao criar, o texto e obrigatorio - quem
    // garante isso e a validacao do DTO de entrada.
    @Column(length = 1000)
    private String text;

    // Guardado, e nao calculado subindo pelos pais: subir a cada leitura seria
    // uma consulta por nivel, e o valor nunca muda depois que a resposta existe.
    @Column(nullable = false)
    private int depth;

    protected ReviewReply() {
    }

    public ReviewReply(Review review, User user, ReviewReply parent, String text) {
        this.review = review;
        this.user = user;
        this.parent = parent;
        this.text = text;
        // O teto e aplicado aqui, no dominio, e nao no service: assim nenhum
        // caminho de escrita consegue criar uma resposta mais funda que o limite.
        this.depth = parent == null ? 0 : Math.min(parent.depth + 1, MAX_DEPTH);
    }

    // Apagar uma resposta que tem filhas nao pode remover a linha: o galho
    // inteiro iria junto, e quem respondeu depois perderia o proprio texto. Some
    // o conteudo, fica o lugar - e a tela mostra "[removido]".
    public void tombstone() {
        this.text = null;
    }

    public boolean isDeleted() {
        return text == null;
    }

    public Long getId() {
        return id;
    }

    public Review getReview() {
        return review;
    }

    public User getUser() {
        return user;
    }

    public ReviewReply getParent() {
        return parent;
    }

    public String getText() {
        return text;
    }

    public int getDepth() {
        return depth;
    }
}
