package com.gamelog.list.controller;

import com.gamelog.list.dto.AddListItemRequest;
import com.gamelog.list.dto.GameListResponse;
import com.gamelog.list.dto.GameListSummary;
import com.gamelog.list.dto.SaveGameListRequest;
import com.gamelog.list.dto.UpdateItemNoteRequest;
import com.gamelog.list.service.GameListService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Rotas das listas tematicas.
//
// Ler e publico; escrever exige login. Mas "publico" aqui nao quer dizer "tudo
// aparece": o Principal chega ANULAVEL nas rotas de leitura, e o service o usa
// pra decidir se as listas privadas do dono entram. Uma lista privada some pra
// qualquer outra pessoa, com 404.
@RestController
public class GameListController {

    private final GameListService gameListService;

    public GameListController(GameListService gameListService) {
        this.gameListService = gameListService;
    }

    // ---------- leitura ----------

    // As listas de alguem. Abre no perfil dele.
    @GetMapping("/api/users/{username}/lists")
    public List<GameListSummary> byOwner(@PathVariable String username, Principal principal) {
        return gameListService.findByOwner(username, nomeDe(principal));
    }

    // Descoberta: quem clica numa tag cai aqui. So publicas.
    @GetMapping("/api/lists")
    public List<GameListSummary> byTag(@RequestParam(required = false) String tag) {
        return gameListService.findByTag(tag);
    }

    @GetMapping("/api/lists/{listId}")
    public GameListResponse byId(@PathVariable Long listId, Principal principal) {
        return gameListService.findById(listId, nomeDe(principal));
    }

    // ---------- escrita ----------

    @PostMapping("/api/lists")
    @ResponseStatus(HttpStatus.CREATED)
    public GameListResponse create(
            @Valid @RequestBody SaveGameListRequest request,
            Principal principal
    ) {
        return gameListService.create(principal.getName(), request);
    }

    @PutMapping("/api/lists/{listId}")
    public GameListResponse update(
            @PathVariable Long listId,
            @Valid @RequestBody SaveGameListRequest request,
            Principal principal
    ) {
        return gameListService.update(principal.getName(), listId, request);
    }

    @DeleteMapping("/api/lists/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long listId, Principal principal) {
        gameListService.delete(principal.getName(), listId);
    }

    // ---------- itens ----------

    // O id da lista fica no caminho tambem nas rotas de item. Nao e redundancia:
    // e o que permite ao service verificar que o item pertence AQUELA lista antes
    // de mexer nele - o id do item sozinho nao prova nada sobre quem pode edita-lo.
    @PostMapping("/api/lists/{listId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public GameListResponse addItem(
            @PathVariable Long listId,
            @Valid @RequestBody AddListItemRequest request,
            Principal principal
    ) {
        return gameListService.addItem(principal.getName(), listId, request);
    }

    @PutMapping("/api/lists/{listId}/items/{itemId}")
    public GameListResponse updateItem(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemNoteRequest request,
            Principal principal
    ) {
        return gameListService.updateItemNote(principal.getName(), listId, itemId, request.note());
    }

    @DeleteMapping("/api/lists/{listId}/items/{itemId}")
    public GameListResponse removeItem(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            Principal principal
    ) {
        // Devolve a lista inteira, e nao 204: tirar um jogo RENUMERA os que
        // sobraram, entao a tela precisa do estado novo de qualquer forma.
        return gameListService.removeItem(principal.getName(), listId, itemId);
    }

    private String nomeDe(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
