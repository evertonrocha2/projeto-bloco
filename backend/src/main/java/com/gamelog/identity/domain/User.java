package com.gamelog.identity.domain;

import com.gamelog.shared.persistence.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Usuario do sistema. Cada um pode escrever reviews e tem um perfil publico.
// @Entity diz pro JPA que essa classe vira uma tabela no banco.
//
// Estende Auditable: created_at e updated_at sao preenchidos automaticamente
// pelo mecanismo de auditoria do Spring Data (ver Auditable).
@Entity
@Table(name = "users")
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true vira uma constraint no banco: e a ULTIMA linha de defesa
    // contra username/email duplicado, mesmo que o service falhe na checagem
    // (ex: duas requisicoes simultaneas). Tambem cria um indice, o que deixa
    // o findByUsername (usado em todo login) rapido.
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    // Guardamos o hash BCrypt da senha, nunca a senha em texto puro.
    @Column(nullable = false)
    private String password;

    @Column(length = 500)
    private String bio;

    // O JPA exige um construtor vazio pra conseguir instanciar a entidade.
    protected User() {
    }

    public User(String username, String email, String password, String bio) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.bio = bio;
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
}
