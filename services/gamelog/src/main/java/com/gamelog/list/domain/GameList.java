package com.gamelog.list.domain;

import com.gamelog.catalog.domain.Game;
import com.gamelog.identity.domain.User;
import com.gamelog.shared.persistence.Auditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

// Uma lista tematica montada por alguem: "os que valeram cada hora", "os que eu
// larguei no tutorial", "pra jogar no sofa com alguem".
//
// Nao e a colecao com outro nome. Na colecao um jogo tem UM status, garantido por
// constraint; numa lista o mesmo jogo aparece em quantas listas a pessoa quiser.
// Sao regras opostas sobre a mesma dupla (pessoa, jogo), e junta-las obrigaria uma
// das duas a ceder.
@Entity
@Audited
@AuditOverride(forClass = Auditable.class)
@Table(
        name = "game_lists",
        indexes = @Index(name = "idx_game_lists_owner_id", columnList = "owner_id")
)
public class GameList extends Auditable {

    // Teto de tags por lista. Tag serve pra agrupar; passando de meia duzia ela
    // vira etiqueta pessoal que nao agrupa nada, e o cartao da lista fica ilegivel.
    public static final int MAX_TAGS = 5;

    private static final int MAX_TAG_LENGTH = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private User owner;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ListVisibility visibility = ListVisibility.PUBLIC;

    // Tags como colecao de valores, e nao entidade Tag propria.
    //
    // Uma entidade so se justificaria com indice global de tags e contagem de uso
    // - uma tela de "tags mais usadas" que ninguem pediu. Do jeito que esta, o
    // JPQL ainda filtra com "member of", que e o que a busca por tag precisa.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "game_list_tags",
            joinColumns = @JoinColumn(name = "list_id"),
            indexes = @Index(name = "idx_game_list_tags_tag", columnList = "tag")
    )
    @Column(name = "tag", length = MAX_TAG_LENGTH, nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    // orphanRemoval: tirar um item da lista tem que apagar a linha. Item de lista
    // nao existe fora dela - sem a lista, ele nao significa nada.
    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    @AuditOverride(forClass = Auditable.class)
    private List<GameListItem> items = new ArrayList<>();

    protected GameList() {
    }

    public GameList(User owner, String title, String description) {
        this.owner = owner;
        this.title = title;
        this.description = description;
    }

    public void update(String title, String description, String coverUrl, ListVisibility visibility) {
        this.title = title;
        this.description = description;
        this.coverUrl = coverUrl;
        this.visibility = visibility;
    }

    // Normaliza na entrada: minusculas, sem espaco nas pontas, sem vazias.
    //
    // Sem isso "Indie", "indie" e " indie " viram tres tags diferentes, e a busca
    // por tag passa a depender de a pessoa ter digitado exatamente igual a quem
    // criou a lista - o que nunca acontece.
    public void setTags(Set<String> novas) {
        this.tags.clear();

        if (novas == null) {
            return;
        }

        novas.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .limit(MAX_TAGS)
                .forEach(this.tags::add);
    }

    // Poe um jogo no fim da lista.
    //
    // A posicao vem do tamanho atual: entrar sempre no fim e o comportamento que
    // nao surpreende ninguem, e evita ter que renumerar o resto a cada insercao.
    public GameListItem addItem(Game game, String note) {
        GameListItem item = new GameListItem(this, game, note, items.size());
        items.add(item);
        return item;
    }

    // Tira um jogo e RENUMERA o que sobrou.
    //
    // Sem renumerar, apagar o item do meio deixaria buracos (0, 1, 3, 4) e o
    // proximo addItem usaria uma posicao ja ocupada - dois itens empatados, com a
    // ordem decidida por sorteio do banco.
    public boolean removeItem(Long itemId) {
        boolean removido = items.removeIf(item -> item.getId().equals(itemId));

        if (removido) {
            for (int posicao = 0; posicao < items.size(); posicao++) {
                items.get(posicao).setPosition(posicao);
            }
        }

        return removido;
    }

    public boolean isOwnedBy(String username) {
        return owner.getUsername().equals(username);
    }

    public boolean isVisibleTo(String username) {
        return visibility == ListVisibility.PUBLIC
                || (username != null && isOwnedBy(username));
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public ListVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ListVisibility visibility) {
        this.visibility = visibility;
    }

    public Set<String> getTags() {
        return tags;
    }

    public List<GameListItem> getItems() {
        return items;
    }
}
