package com.gamelog.identity.controller;

import com.gamelog.identity.dto.AuthResponse;
import com.gamelog.identity.dto.LoginRequest;
import com.gamelog.identity.dto.RegisterRequest;
import com.gamelog.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Porta de entrada da autenticacao. O controller e fino de proposito: ele so
// recebe a requisicao, repassa pro service e devolve a resposta. Toda a regra
// fica no AuthService.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // @Valid dispara as validacoes das anotacoes do RegisterRequest.
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
