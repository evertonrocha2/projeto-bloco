package com.gamelog.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Dados que chegam no cadastro. Uso um record porque e so um pacote imutavel
// de dados - sem logica. As anotacoes validam a entrada antes de chegar no service.
public record RegisterRequest(
        @NotBlank(message = "username e obrigatorio")
        @Size(min = 3, max = 30, message = "username deve ter entre 3 e 30 caracteres")
        String username,

        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email invalido")
        String email,

        @NotBlank(message = "senha e obrigatoria")
        @Size(min = 6, max = 100, message = "senha deve ter no minimo 6 caracteres")
        String password,

        @Size(max = 500, message = "bio muito longa")
        String bio
) {
}
