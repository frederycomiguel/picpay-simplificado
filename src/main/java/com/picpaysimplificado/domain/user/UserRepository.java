package com.picpaysimplificado.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * [PT-BR] Busca um usuário pelo CPF ou CNPJ (documento).
     * [EN]    Finds a user by their CPF or CNPJ (document).
     *
     * @param document CPF ou CNPJ / Document number
     * @return Optional contendo o usuário se encontrado / Optional with user if found
     */
    Optional<User> findByDocument(String document);

    /**
     * [PT-BR] Busca um usuário pelo endereço de e-mail.
     * [EN]    Finds a user by their email address.
     *
     * @param email Endereço de e-mail / Email address
     * @return Optional contendo o usuário se encontrado / Optional with user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * [PT-BR] Verifica se já existe um usuário cadastrado com o CPF/CNPJ informado.
     * [EN]    Checks if a user already exists with the given document.
     *
     * @param document CPF ou CNPJ / Document number
     * @return true se já existir, false caso contrário / true if exists, false otherwise
     */
    boolean existsByDocument(String document);

    /**
     * [PT-BR] Verifica se já existe um usuário cadastrado com o e-mail informado.
     * [EN]    Checks if a user already exists with the given email.
     *
     * @param email Endereço de e-mail / Email address
     * @return true se já existir, false caso contrário / true if exists, false otherwise
     */
    boolean existsByEmail(String email);
}
