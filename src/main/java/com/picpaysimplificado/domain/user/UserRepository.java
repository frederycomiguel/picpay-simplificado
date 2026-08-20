package com.picpaysimplificado.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByDocument(String document);

    Optional<User> findByEmail(String email);

    boolean existsByDocument(String document);

    boolean existsByEmail(String email);
}
