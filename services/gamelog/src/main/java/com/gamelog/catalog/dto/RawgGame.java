package com.gamelog.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Um jogo como vem na LISTA da RAWG. A lista nao traz a descricao completa
// (isso vem no detalhe), entao aqui ficam so os campos do card: nome, imagem,
// data, nota da comunidade RAWG e os generos.
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgGame(
        Long id,
        String name,
        String released,
        @JsonProperty("background_image") String backgroundImage,
        double rating,
        List<RawgGenre> genres
) {
}
