package com.gamelog.list.dto;

import com.gamelog.list.domain.ListVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

// O que a tela manda pra criar ou editar uma lista.
//
// Serve pros dois casos de proposito: criar e editar preenchem exatamente os
// mesmos campos, e dois records identicos so criariam a duvida de qual usar.
//
// visibility anulavel significa "publica", que e o padrao do app.
public record SaveGameListRequest(

        @NotBlank(message = "a lista precisa de um titulo")
        @Size(max = 120, message = "o titulo passou de 120 caracteres")
        String title,

        @Size(max = 2000, message = "a descricao passou de 2000 caracteres")
        String description,

        String coverUrl,

        Set<String> tags,

        ListVisibility visibility
) {
}
