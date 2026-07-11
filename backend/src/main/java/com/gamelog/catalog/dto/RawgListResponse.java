package com.gamelog.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// O "envelope" que a RAWG usa na listagem: os jogos vem dentro de "results".
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgListResponse(List<RawgGame> results) {
}
