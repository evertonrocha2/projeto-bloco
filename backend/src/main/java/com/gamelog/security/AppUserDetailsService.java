package com.gamelog.security;

import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

// Ponte entre o nosso User (do dominio) e o User que o Spring Security entende.
// O Spring chama isso pra carregar o usuario pelo username; a gente busca no
// banco e devolve no formato que ele espera.
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + username));

        // Usamos a classe pronta do proprio Spring Security. Sem papeis/roles
        // porque o app nao precisa deles nesta entrega - so logado x deslogado.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.emptyList()
        );
    }
}
