package com.gamelog.identity.dto;

// Resposta do login/cadastro: o token JWT que o front guarda e usa nas
// proximas requisicoes, mais o username pra exibir na tela.
public record AuthResponse(String token, String username) {
}
