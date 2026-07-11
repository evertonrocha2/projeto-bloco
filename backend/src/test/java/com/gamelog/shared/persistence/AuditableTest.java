package com.gamelog.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// Prova que a auditoria automatica do Spring Data esta funcionando: ninguem
// seta data nenhuma na mao, e mesmo assim created_at e updated_at aparecem
// preenchidos no banco.
@DataJpaTest
class AuditableTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void preencheCreatedAtEUpdatedAtSozinho() {
        User salvo = userRepository.saveAndFlush(
                new User("erin", "erin@email.com", "hash", null));

        assertThat(salvo.getCreatedAt()).isNotNull();
        assertThat(salvo.getUpdatedAt()).isNotNull();
    }

    @Test
    void updatedAtAvancaQuandoAEntidadeMuda() throws InterruptedException {
        User salvo = userRepository.saveAndFlush(
                new User("fabio", "fabio@email.com", "hash", null));
        var criadoEm = salvo.getCreatedAt();
        var atualizadoEm = salvo.getUpdatedAt();

        // Pausa minima pra garantir que o relogio anda entre o insert e o update.
        Thread.sleep(10);

        salvo.setBio("agora tenho bio");
        User atualizado = userRepository.saveAndFlush(salvo);

        // created_at nao muda nunca; updated_at acompanha a mudanca.
        assertThat(atualizado.getCreatedAt()).isEqualTo(criadoEm);
        assertThat(atualizado.getUpdatedAt()).isAfter(atualizadoEm);
    }
}
