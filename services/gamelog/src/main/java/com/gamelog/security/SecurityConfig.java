package com.gamelog.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Aqui a gente configura toda a politica de seguranca da API: o que e publico,
// o que exige login, como tratar CORS e onde encaixar o filtro de JWT.
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Habilita CORS (config logo abaixo) pro front em outra porta conseguir chamar a API.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF e protecao pra formularios com sessao/cookie. Nossa API e stateless
                // e usa token no header, entao nao faz sentido e a gente desliga.
                .csrf(AbstractHttpConfigurer::disable)
                // Sem sessao no servidor: cada requisicao se identifica pelo token.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Login e cadastro tem que ser publicos (senao ninguem entra).
                        .requestMatchers("/api/auth/**").permitAll()
                        // "Meu perfil" depende de quem esta logado, entao exige token.
                        // Precisa vir ANTES da regra geral de /api/users/** abaixo.
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        // Ver catalogo e perfis e publico (leitura). Escrever exige login.
                        .requestMatchers(HttpMethod.GET, "/api/games/**", "/api/users/**").permitAll()
                        // Console do H2 liberado pra inspecionar o banco em desenvolvimento.
                        .requestMatchers("/h2-console/**").permitAll()
                        // Endpoints de monitoramento liberados. Sao consultados por
                        // OUTROS PROCESSOS (o Eureka pra saber se a instancia esta
                        // saudavel, o gateway pra decidir se manda trafego), e esses
                        // processos nao tem token de usuario nenhum pra apresentar.
                        // Sem esta regra, a chamada de health caia na regra geral
                        // "anyRequest().authenticated()" e voltava 403 - o servico
                        // parecia doente estando perfeitamente no ar.
                        //
                        // O que fica exposto e so o que application.properties permite
                        // (management.endpoints.web.exposure.include=health,info), e nao
                        // o conjunto completo do Actuator.
                        .requestMatchers("/actuator/**").permitAll()
                        // Qualquer outra coisa (ex: postar review, ver /api/me) exige estar logado.
                        .anyRequest().authenticated())
                // O console do H2 roda dentro de um frame; sem isso o navegador bloqueia.
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                // Encaixa nosso filtro de JWT antes do filtro padrao de usuario/senha.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // BCrypt pra transformar a senha num hash. O mesmo bean e usado no cadastro
    // (pra gerar o hash) e no login (pra comparar).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Permite que o front (Vite, em localhost:5173) chame a API em localhost:8080.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
