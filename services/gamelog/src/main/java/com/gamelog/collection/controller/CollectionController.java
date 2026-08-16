package com.gamelog.collection.controller;

import com.gamelog.collection.dto.AddToCollectionRequest;
import com.gamelog.collection.dto.CollectionEntryResponse;
import com.gamelog.collection.dto.CollectionRevisionResponse;
import com.gamelog.collection.service.CollectionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    // Adiciona/atualiza um jogo na colecao de quem esta logado. Exige token: o
    // Principal diz quem e o dono da colecao, sem precisar mandar no corpo.
    @PostMapping("/api/collection")
    public CollectionEntryResponse addOrUpdate(
            @Valid @RequestBody AddToCollectionRequest request,
            Principal principal
    ) {
        return collectionService.addOrUpdate(principal.getName(), request);
    }

    // Colecao publica de um usuario - aparece no perfil dele.
    @GetMapping("/api/users/{username}/collection")
    public List<CollectionEntryResponse> getCollection(@PathVariable String username) {
        return collectionService.findByUsername(username);
    }

    // Historico de um item da minha colecao: cada mudanca de horas/status vira
    // uma revisao, entao da pra ver a jornada com o jogo ("quando eu zerei?").
    @GetMapping("/api/collection/{entryId}/history")
    public List<CollectionRevisionResponse> history(@PathVariable Long entryId, Principal principal) {
        return collectionService.history(principal.getName(), entryId);
    }
}
