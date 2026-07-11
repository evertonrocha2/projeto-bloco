package com.gamelog.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "username e obrigatorio")
        String username,

        @NotBlank(message = "senha e obrigatoria")
        String password
) {
}
