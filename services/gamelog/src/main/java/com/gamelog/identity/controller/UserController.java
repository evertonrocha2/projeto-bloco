package com.gamelog.identity.controller;

import com.gamelog.identity.dto.UpdateProfileRequest;
import com.gamelog.identity.dto.UserProfileResponse;
import com.gamelog.identity.dto.UserStatsResponse;
import com.gamelog.identity.service.UserService;
import com.gamelog.identity.service.UserStatsService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserStatsService userStatsService;

    public UserController(UserService userService, UserStatsService userStatsService) {
        this.userService = userService;
        this.userStatsService = userStatsService;
    }

    // Perfil publico de qualquer usuario - e isso que abre quando voce clica no
    // nome de alguem numa review.
    //
    // O Principal e anulavel: a rota e publica. Quando existe, e usado so pra
    // marcar os polegares de quem esta olhando, nunca pra decidir o que mostrar.
    @GetMapping("/{username}")
    public UserProfileResponse getProfile(@PathVariable String username, Principal principal) {
        return userService.getProfile(username, principal == null ? null : principal.getName());
    }

    // "Meu perfil". O Principal e injetado pelo Spring Security e carrega o
    // username de quem mandou o token, entao aqui a gente sabe quem esta logado.
    @GetMapping("/me")
    public UserProfileResponse getMe(Principal principal) {
        return userService.getProfile(principal.getName(), principal.getName());
    }

    // Os numeros do perfil: horas, jogos por status, genero favorito, nota media,
    // conquistas e a retrospectiva do ano.
    //
    // Um endpoint so. A alternativa seria a tela abrir cinco requisicoes ao
    // carregar, e as cinco leem as mesmas duas tabelas.
    @GetMapping("/{username}/stats")
    public UserStatsResponse getStats(@PathVariable String username) {
        return userStatsService.forUser(username);
    }

    // Editar o proprio perfil: bio, avatar e capa.
    //
    // Nao existe rota pra editar o perfil de outra pessoa, e o username vem do
    // Principal - nunca do corpo nem do caminho. Aceitar o alvo por parametro
    // criaria uma rota em que esquecer UMA comparacao deixa qualquer um editar
    // qualquer perfil.
    @PutMapping("/me")
    public UserProfileResponse updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal
    ) {
        return userService.updateProfile(principal.getName(), request);
    }
}
