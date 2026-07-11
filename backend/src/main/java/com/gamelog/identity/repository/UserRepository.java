package com.gamelog.identity.repository;

import com.gamelog.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Estendendo JpaRepository a gente ja ganha de graca save, findById, findAll,
// delete, etc. So precisamos declarar as consultas extras que queremos - o
// Spring Data implementa elas sozinho a partir do nome do metodo.
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
