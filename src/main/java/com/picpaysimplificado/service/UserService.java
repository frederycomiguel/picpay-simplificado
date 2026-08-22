package com.picpaysimplificado.service;

import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserRepository;
import com.picpaysimplificado.dto.LoginRequest;
import com.picpaysimplificado.dto.UserCreateRequest;
import com.picpaysimplificado.dto.UserResponse;
import com.picpaysimplificado.infra.exception.InvalidCredentialsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * [PT-BR] Cadastra um novo usuário no sistema com validação de duplicidade para CPF/CNPJ e e-mail.
     * [EN]    Registers a new user in the system with uniqueness validation for document (CPF/CNPJ) and email.
     *
     * @param request DTO com os dados do usuário / User registration DTO
     * @return DTO com os dados do usuário salvo / Saved user response DTO
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Criando usuário: {} {}", request.firstName(), request.lastName());

        if (userRepository.existsByDocument(request.document())) {
            throw new IllegalArgumentException("Já existe um usuário com este documento: " + request.document());
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe um usuário com este e-mail: " + request.email());
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .document(request.document())
                .email(request.email())
                .password(request.password()) // In production, hash with BCrypt
                .balance(request.balance())
                .userType(request.userType())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Usuário criado com sucesso. ID: {}", savedUser.getId());

        return toResponse(savedUser);
    }

    /**
     * [PT-BR] Retorna a lista de todos os usuários cadastrados.
     * [EN]    Returns the list of all registered users.
     *
     * @return Lista de DTOs de usuários / List of user DTOs
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * [PT-BR] Busca os dados públicos de um usuário por ID e converte para DTO de resposta.
     * [EN]    Retrieves public data of a user by ID and converts it to a response DTO.
     *
     * @param id ID do usuário / User ID
     * @return DTO com informações do usuário / DTO with user information
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findById(id);
        return toResponse(user);
    }

    /**
     * [PT-BR] Autentica um usuário validando e-mail e senha. Usada pela tela de login do front-end,
     *         já que esta API não expõe um mecanismo de sessão/token — apenas essa checagem pontual.
     * [EN]    Authenticates a user by validating email and password. Used by the front-end login screen,
     *         since this API exposes no session/token mechanism — just this one-off check.
     *
     * @param request DTO com e-mail e senha / DTO with email and password
     * @return DTO com os dados do usuário autenticado / Authenticated user response DTO
     */
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("E-mail ou senha inválidos"));

        if (!user.getPassword().equals(request.password())) {
            throw new InvalidCredentialsException("E-mail ou senha inválidos");
        }

        return toResponse(user);
    }

    /**
     * [PT-BR] Busca a entidade User pelo ID ou lança EntityNotFoundException caso não exista.
     * [EN]    Finds User entity by ID or throws EntityNotFoundException if not found.
     *
     * @param id ID do usuário / User ID
     * @return Entidade User / User entity
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + id));
    }

    /**
     * [PT-BR] Salva ou atualiza a entidade de usuário no banco de dados (usado para debitar/creditar saldo).
     * [EN]    Saves or updates user entity in the database (used to debit/credit balance).
     *
     * @param user Entidade do usuário / User entity
     */
    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    /**
     * [PT-BR] Método utilitário privado para converter a entidade User em UserResponse (omitindo senha).
     * [EN]    Private helper method to convert User entity into UserResponse DTO (omitting password).
     *
     * @param user Entidade do usuário / User entity
     * @return DTO de resposta do usuário / User response DTO
     */
    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getDocument(),
                user.getEmail(),
                user.getBalance(),
                user.getUserType(),
                user.getCreatedAt()
        );
    }
}
