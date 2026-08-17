package com.gamelog.list.dto;

import com.gamelog.list.domain.GameList;
import java.time.Instant;
import java.util.Set;

// Uma lista no cartao do perfil ou na busca por tag: cabecalho e quantos jogos
// tem, sem os jogos.
//
// Existe separado do GameListResponse porque a aba de listas do perfil mostra N
// cartoes. Mandar os itens de todas as listas ali traria a colecao inteira da
// pessoa pra desenhar um numero.
public record GameListSummary(
        Long id,
        String owner,
        String title,
        String description,
        String coverUrl,
        String visibility,
        Set<String> tags,
        long gameCount,
        Instant createdAt
) {
    public static GameListSummary from(GameList list, long gameCount) {
        return new GameListSummary(
                list.getId(),
                list.getOwner().getUsername(),
                list.getTitle(),
                list.getDescription(),
                list.getCoverUrl(),
                list.getVisibility().name(),
                list.getTags(),
                gameCount,
                list.getCreatedAt()
        );
    }
}
