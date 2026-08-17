package com.gamelog.list.domain;

import com.gamelog.catalog.domain.Game;
import com.gamelog.shared.persistence.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

// Um jogo dentro de uma lista, com o comentario de quem o colocou ali.
//
// A nota e o que separa uma lista tematica de uma pasta de capas: "esse aqui e o
// motivo da lista existir" e a informacao que faz alguem parar pra ler. Sem ela
// sobra uma grade de imagens que nao explica nada.
@Entity
@Audited
@AuditOverride(forClass = Auditable.class)
@Table(
        name = "game_list_items",
        // Um jogo nao se repete dentro da mesma lista. Entre listas diferentes,
        // repete a vontade - e a diferenca central pra colecao.
        uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "game_id"})
)
public class GameListItem extends Auditable {

    public static final int MAX_NOTE_LENGTH = 280;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id")
    private GameList list;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Game game;

    @Column(length = MAX_NOTE_LENGTH)
    private String note;

    // Ordem dentro da lista. Guardada, e nao derivada da data de insercao, pra
    // reordenar ser possivel sem reescrever historico.
    @Column(nullable = false)
    private int position;

    protected GameListItem() {
    }

    GameListItem(GameList list, Game game, String note, int position) {
        this.list = list;
        this.game = game;
        this.note = note;
        this.position = position;
    }

    public void setNote(String note) {
        this.note = note;
    }

    void setPosition(int position) {
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public GameList getList() {
        return list;
    }

    public Game getGame() {
        return game;
    }

    public String getNote() {
        return note;
    }

    public int getPosition() {
        return position;
    }
}
