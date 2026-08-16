package com.gamelog.integration.controller;

import com.gamelog.integration.dto.GameActivityResponse;
import com.gamelog.integration.service.GameActivityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Porta de entrada do monolito para OUTROS SERVICOS.
//
// Antes do TP3 o monolito tinha um unico tipo de cliente: o front React. Agora
// tem dois, e com necessidades diferentes. O modulo "integration" existe pra
// deixar isso explicito no codigo - quem abrir o projeto ve, pela estrutura de
// pastas, que existe uma superficie voltada a integracao entre servicos.
//
// A rota fica sob /api/users porque, do ponto de vista de REST, isto e um
// recurso do usuario. A pasta e que reflete o proposito: servir outro servico.
@RestController
public class GameActivityController {

    private final GameActivityService gameActivityService;

    public GameActivityController(GameActivityService gameActivityService) {
        this.gameActivityService = gameActivityService;
    }

    // Consumido pelo recommendation-service via OpenFeign.
    // Leitura publica, igual ao resto de /api/users/** - nao expoe nada que o
    // perfil publico ja nao mostre.
    @GetMapping("/api/users/{username}/game-activity")
    public GameActivityResponse getGameActivity(@PathVariable String username) {
        return gameActivityService.getActivity(username);
    }
}
