package com.gamelog.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// Responsavel unico por mexer com tokens JWT: gerar quando o usuario loga e
// ler/validar quando uma requisicao chega. Concentrar isso aqui mantem o resto
// do codigo sem saber os detalhes da biblioteca de JWT.
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    // Os valores vem do application.properties via @Value.
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        // A chave secreta vira uma SecretKey usada pra assinar e conferir o token.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // Gera um token assinado guardando o username no "subject".
    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    // Le o username de dentro do token. Se a assinatura nao bater ou o token
    // estiver expirado, a propria biblioteca lanca excecao.
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Diz se o token e valido sem deixar a excecao vazar.
    public boolean isValid(String token) {
        try {
            extractUsername(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
