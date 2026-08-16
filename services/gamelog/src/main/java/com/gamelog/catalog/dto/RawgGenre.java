package com.gamelog.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Um genero ("topico") como a RAWG devolve. So precisamos do nome.
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgGenre(String name) {
}
