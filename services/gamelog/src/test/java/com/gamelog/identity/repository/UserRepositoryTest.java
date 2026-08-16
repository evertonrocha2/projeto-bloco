package com.gamelog.identity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamelog.identity.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

// @DataJpaTest sobe so a fatia de persistencia (entidades + repositorios) com
// um banco H2 em memoria zerado. Cada teste roda dentro de uma transacao que e
// desfeita no final, entao um teste nunca enxerga a sujeira do outro.
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void salvaEBuscaPorUsername() {
        userRepository.save(new User("alice", "alice@email.com", "hash", "oi, sou a alice"));

        var found = userRepository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@email.com");
    }

    @Test
    void existsRespondeCertoParaUsernameEEmail() {
        userRepository.save(new User("bob", "bob@email.com", "hash", null));

        assertThat(userRepository.existsByUsername("bob")).isTrue();
        assertThat(userRepository.existsByEmail("bob@email.com")).isTrue();
        assertThat(userRepository.existsByUsername("ninguem")).isFalse();
        assertThat(userRepository.existsByEmail("ninguem@email.com")).isFalse();
    }

    @Test
    void bancoRejeitaUsernameDuplicado() {
        // A constraint unique do banco e a ultima linha de defesa contra
        // duplicados - aqui a gente prova que ela realmente existe.
        userRepository.saveAndFlush(new User("carla", "carla@email.com", "hash", null));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(new User("carla", "outra@email.com", "hash", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void naoAchaQuemNaoExiste() {
        assertThat(userRepository.findByUsername("fantasma")).isEmpty();
    }
}
