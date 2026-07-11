package com.gamelog.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// Usuario do sistema. Cada um pode escrever reviews e tem um perfil publico.
// @Entity diz pro JPA que essa classe vira uma tabela no banco.
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    // Guardamos o hash BCrypt da senha, nunca a senha em texto puro.
    @Column(nullable = false)
    private String password;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // O JPA exige um construtor vazio pra conseguir instanciar a entidade.
    protected User() {
    }

    public User(String username, String email, String password, String bio) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.bio = bio;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
