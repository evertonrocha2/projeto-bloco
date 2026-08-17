package com.gamelog.list.service;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.list.domain.GameList;
import com.gamelog.list.domain.GameListItem;
import com.gamelog.list.domain.ListVisibility;
import com.gamelog.list.dto.AddListItemRequest;
import com.gamelog.list.dto.GameListResponse;
import com.gamelog.list.dto.GameListSummary;
import com.gamelog.list.dto.SaveGameListRequest;
import com.gamelog.list.repository.GameListRepository;
import com.gamelog.shared.BadRequestException;
import com.gamelog.shared.ImageUrl;
import com.gamelog.shared.NotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// As listas tematicas: criar, editar, povoar e descobrir.
//
// A regra que atravessa o arquivo inteiro e a de visibilidade. Lista privada
// responde 404, e nao 403, pra quem nao e o dono - negar permissao confirmaria
// que ela existe, e "essa lista existe mas voce nao pode ver" ja e informacao
// sobre algo que alguem marcou como so seu.
//
// Isso vale inclusive pras operacoes de escrita: tentar editar a lista de outra
// pessoa devolve 404, pelo mesmo motivo.
@Service
public class GameListService {

    private final GameListRepository listRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public GameListService(GameListRepository listRepository,
                           UserRepository userRepository,
                           GameRepository gameRepository) {
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    // ---------- escrita ----------

    @Transactional
    public GameListResponse create(String username, SaveGameListRequest request) {
        User owner = findUser(username);

        GameList list = new GameList(owner, request.title(), request.description());
        aplicar(list, request);

        return GameListResponse.from(listRepository.save(list));
    }

    @Transactional
    public GameListResponse update(String username, Long listId, SaveGameListRequest request) {
        GameList list = findOwned(username, listId);
        aplicar(list, request);

        return GameListResponse.from(listRepository.save(list));
    }

    @Transactional
    public void delete(String username, Long listId) {
        listRepository.delete(findOwned(username, listId));
    }

    // Campos que criar e editar tratam igual.
    private void aplicar(GameList list, SaveGameListRequest request) {
        Set<String> tags = request.tags() == null ? Set.of() : request.tags();

        // Recusa, e nao corta em silencio. Quem digitou seis tags precisa saber
        // que a sexta nao entrou - senao vai procurar por ela depois e concluir
        // que a busca esta quebrada.
        if (tags.size() > GameList.MAX_TAGS) {
            throw new BadRequestException("Uma lista aceita no maximo " + GameList.MAX_TAGS + " tags");
        }

        ListVisibility visibility =
                request.visibility() == null ? ListVisibility.PUBLIC : request.visibility();

        list.update(
                request.title(),
                request.description(),
                // A capa vai parar num <img src> de uma pagina que outras pessoas
                // abrem. Mesma checagem do avatar.
                ImageUrl.sanitize(request.coverUrl()),
                visibility
        );
        list.setTags(tags);
    }

    // ---------- itens ----------

    @Transactional
    public GameListResponse addItem(String username, Long listId, AddListItemRequest request) {
        GameList list = findOwned(username, listId);

        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new NotFoundException("Jogo nao encontrado"));

        // Checagem antes da constraint pra a mensagem ser util. A constraint
        // continua sendo a garantia real, contra dois cliques simultaneos.
        boolean jaEsta = list.getItems().stream()
                .anyMatch(item -> item.getGame().getId().equals(game.getId()));

        if (jaEsta) {
            throw new BadRequestException("Esse jogo ja esta na lista");
        }

        list.addItem(game, request.note());
        return GameListResponse.from(listRepository.save(list));
    }

    @Transactional
    public GameListResponse updateItemNote(String username, Long listId, Long itemId, String note) {
        GameList list = findOwned(username, listId);

        GameListItem item = acharItem(list, itemId);
        item.setNote(note);

        return GameListResponse.from(listRepository.save(list));
    }

    @Transactional
    public GameListResponse removeItem(String username, Long listId, Long itemId) {
        GameList list = findOwned(username, listId);

        if (!list.removeItem(itemId)) {
            throw new NotFoundException("Item nao encontrado nessa lista");
        }

        return GameListResponse.from(listRepository.save(list));
    }

    // O item tem que pertencer A ESTA lista.
    //
    // Sem esta checagem, mandar o id de um item qualquer junto do id de uma lista
    // minha deixaria editar a nota de qualquer pessoa - o id do item sozinho nao
    // prova nada sobre quem pode mexer nele.
    private GameListItem acharItem(GameList list, Long itemId) {
        return list.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Item nao encontrado nessa lista"));
    }

    // ---------- leitura ----------

    @Transactional(readOnly = true)
    public GameListResponse findById(Long listId, String viewerUsername) {
        GameList list = listRepository.findById(listId)
                .orElseThrow(GameListService::naoEncontrada);

        if (!list.isVisibleTo(viewerUsername)) {
            throw naoEncontrada();
        }

        return GameListResponse.from(list);
    }

    // As listas que aparecem num perfil.
    //
    // O dono ve as proprias privadas; qualquer outra pessoa ve so as publicas. A
    // decisao e por CONSULTA, e nao por filtro depois: a consulta que traz tudo e
    // filtra em memoria e a que um refactor futuro esquece de filtrar.
    @Transactional(readOnly = true)
    public List<GameListSummary> findByOwner(String ownerUsername, String viewerUsername) {
        User owner = findUser(ownerUsername);
        boolean ehODono = owner.getUsername().equals(viewerUsername);

        List<GameList> lists = ehODono
                ? listRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                : listRepository.findByOwnerIdAndVisibilityOrderByCreatedAtDesc(
                        owner.getId(), ListVisibility.PUBLIC);

        return resumir(lists);
    }

    // Descoberta por tag. So publicas - a consulta ja garante isso.
    @Transactional(readOnly = true)
    public List<GameListSummary> findByTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return List.of();
        }

        // As tags sao guardadas normalizadas; a busca normaliza igual, pra quem
        // digita "INDIE" achar o mesmo que quem digita "indie".
        return resumir(listRepository.findPublicByTag(tag.trim().toLowerCase(Locale.ROOT)));
    }

    // Monta os resumos com a contagem de jogos vinda de UMA consulta agrupada.
    //
    // Contar item por lista seria uma consulta por cartao do perfil. Aqui sao
    // duas no total, independente de quantas listas a pessoa tenha.
    private List<GameListSummary> resumir(List<GameList> lists) {
        if (lists.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> contagens = new HashMap<>();
        for (Object[] linha : listRepository.countItemsByListIds(
                lists.stream().map(GameList::getId).toList())) {
            contagens.put((Long) linha[0], (Long) linha[1]);
        }

        // getOrDefault: lista vazia nao produz linha no group by, e precisa sair
        // como zero em vez de sumir do perfil.
        return lists.stream()
                .map(list -> GameListSummary.from(list, contagens.getOrDefault(list.getId(), 0L)))
                .toList();
    }

    // Carrega a lista garantindo que ela e de quem esta chamando.
    //
    // Devolve 404 tambem quando a lista existe mas e de outra pessoa: um 403 aqui
    // deixaria descobrir quais ids existem, testando um por um.
    private GameList findOwned(String username, Long listId) {
        GameList list = listRepository.findById(listId)
                .orElseThrow(GameListService::naoEncontrada);

        if (!list.isOwnedBy(username)) {
            throw naoEncontrada();
        }

        return list;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));
    }

    private static NotFoundException naoEncontrada() {
        return new NotFoundException("Lista nao encontrada");
    }
}
