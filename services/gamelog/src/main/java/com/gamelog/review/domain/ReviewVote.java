package com.gamelog.review.domain;

import com.gamelog.identity.domain.User;
import com.gamelog.shared.persistence.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// O voto de alguem numa avaliacao alheia.
//
// A constraint de unicidade (user_id, review_id) e a regra "um voto por pessoa
// por avaliacao" escrita onde ela nao pode ser burlada. O service tambem checa,
// mas dois cliques rapidos viram duas requisicoes simultaneas, e nesse caso as
// duas passam pela checagem antes de qualquer uma gravar.
//
// NAO e @Audited, ao contrario de Review e CollectionEntry. Um voto e um fato
// binario e sem historia: saber que alguem trocou de positivo pra negativo as
// 14h32 nao serve a nenhuma tela nem a nenhuma regra. Auditar seria dobrar a
// escrita da tabela mais escrita do app em troca de dado que ninguem le.
@Entity
@Table(
        name = "review_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "review_id"}),
        // A pagina de um jogo agrupa votos por avaliacao. A constraint acima ja
        // indexa user_id; este indice cobre o outro lado.
        indexes = @Index(name = "idx_review_votes_review_id", columnList = "review_id")
)
public class ReviewVote extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id")
    private Review review;

    // STRING e nao ORDINAL: com ordinal, inserir um valor no meio do enum
    // reescreveria o significado de todas as linhas ja gravadas.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VoteType type;

    protected ReviewVote() {
    }

    public ReviewVote(User user, Review review, VoteType type) {
        this.user = user;
        this.review = review;
        this.type = type;
    }

    // Trocar de lado muda so o tipo. Autor e avaliacao sao a identidade do voto -
    // mudar qualquer um dos dois seria outro voto, nao este.
    public void changeTo(VoteType type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Review getReview() {
        return review;
    }

    public VoteType getType() {
        return type;
    }
}
