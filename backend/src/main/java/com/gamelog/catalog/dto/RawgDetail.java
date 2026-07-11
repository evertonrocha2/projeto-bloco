package com.gamelog.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// O DETALHE de um jogo na RAWG. So buscamos isso pra pegar a descricao em texto
// limpo (description_raw), que a listagem nao traz.
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgDetail(
        @JsonProperty("description_raw") String descriptionRaw
) {
}
