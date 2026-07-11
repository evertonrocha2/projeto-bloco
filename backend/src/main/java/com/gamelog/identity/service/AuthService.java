package com.gamelog.identity.service;

import com.gamelog.identity.domain.User;
import com.gamelog.identity.dto.AuthResponse;
import com.gamelog.identity.dto.LoginRequest;
import com.gamelog.identity.dto.RegisterRequest;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.security.JwtService;
import com.gamelog.shared.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Cuida do cadastro e do login. A regra e simples: no cadastro a gente garante
// que username/email sao unicos e guarda a senha como hash; no login a gente
// confere a senha e, dando certo, devolve um token JWT.
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Esse username ja esta em uso");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Esse email ja esta cadastrado");
        }

        // Nunca guardamos a senha pura: passamos pelo BCrypt antes de salvar.
        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.username(), request.email(), hashedPassword, request.bio());
        userRepository.save(user);

        // Ja cadastrou? Ja entra logado: devolvemos o token na hora.
        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("Usuario ou senha invalidos"));

        // matches compara a senha digitada com o hash guardado.
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            // Mensagem generica de proposito: nao entregamos se foi o usuario ou a senha que errou.
            throw new BadRequestException("Usuario ou senha invalidos");
        }

        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }
}
