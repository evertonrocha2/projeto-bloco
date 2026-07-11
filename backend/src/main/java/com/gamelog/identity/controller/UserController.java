package com.gamelog.identity.controller;

import com.gamelog.identity.dto.UserProfileResponse;
import com.gamelog.identity.service.UserService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Perfil publico de qualquer usuario - e isso que abre quando voce clica no
    // nome de alguem numa review.
    @GetMapping("/{username}")
    public UserProfileResponse getProfile(@PathVariable String username) {
        return userService.getProfile(username);
    }

    // "Meu perfil". O Principal e injetado pelo Spring Security e carrega o
    // username de quem mandou o token, entao aqui a gente sabe quem esta logado.
    @GetMapping("/me")
    public UserProfileResponse getMe(Principal principal) {
        return userService.getProfile(principal.getName());
    }
}
