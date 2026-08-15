package com.gamelog.recommendation.client;

import com.gamelog.recommendation.domain.CatalogGame;
import com.gamelog.recommendation.domain.GameActivity;
import java.util.List;

// O retrato do GameLog num instante: o que o usuario fez e o catalogo disponivel.
//
// As duas informacoes andam juntas porque uma sem a outra nao serve pra nada -
// atividade sem catalogo nao tem candidatos, catalogo sem atividade nao tem
// perfil. Buscar as duas como uma unidade tambem deixa claro que, se qualquer uma
// das chamadas falhar, o resultado inteiro e inutilizavel e o fallback deve agir.
public record GameLogSnapshot(GameActivity activity, List<CatalogGame> catalog) {
}
