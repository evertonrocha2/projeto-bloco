package com.gamelog.list.domain;

// Quem enxerga uma lista.
//
// O resto do app nao tem nada privado - catalogo, perfis e avaliacoes sao
// publicos. A lista e a excecao porque nem toda lista nasce pra ser lida: "jogos
// que eu abandonei", "presentes pra comprar" e um rascunho de fim de ano sao usos
// legitimos que a pessoa nao quer no perfil.
public enum ListVisibility {

    // Aparece no perfil e na busca por tag.
    PUBLIC,

    // So o dono ve, inclusive pela API. Pra qualquer outro, a lista responde 404 e
    // nao 403 - 403 confirmaria que ela existe.
    PRIVATE
}
