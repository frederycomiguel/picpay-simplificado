package com.picpaysimplificado.controller;

import com.picpaysimplificado.dto.LoginRequest;
import com.picpaysimplificado.dto.UserCreateRequest;
import com.picpaysimplificado.dto.UserResponse;
import com.picpaysimplificado.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Operações de CRUD de usuários")
public class UserController {

    private final UserService userService;

    /**
     * [PT-BR] Endpoint para cadastrar um novo usuário (tipo COMUM ou LOJISTA).
     * [EN]    Endpoint to register a new user (COMMON or MERCHANT type).
     *
     * @param request Dados de cadastro do usuário / User registration data
     * @return Dados do usuário criado com HTTP 201 / Created user details with HTTP 201
     */
    @PostMapping
    @Operation(summary = "Criar usuário", description = "Cria um novo usuário (comum ou lojista)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou documento/email já existente")
    })
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * [PT-BR] Endpoint para obter a lista de todos os usuários cadastrados.
     * [EN]    Endpoint to retrieve the list of all registered users.
     *
     * @return Lista de usuários / List of users
     */
    @GetMapping
    @Operation(summary = "Listar usuários", description = "Lista todos os usuários cadastrados")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * [PT-BR] Endpoint para buscar um usuário específico pelo seu identificador (ID).
     * [EN]    Endpoint to find a specific user by their identifier (ID).
     *
     * @param id ID do usuário / User ID
     * @return Dados do usuário encontrado / Found user details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário", description = "Busca um usuário pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * [PT-BR] Endpoint para autenticar um usuário por e-mail e senha (usado pela tela de login do front-end).
     * [EN]    Endpoint to authenticate a user by email and password (used by the front-end login screen).
     *
     * @param request Credenciais de login / Login credentials
     * @return Dados do usuário autenticado / Authenticated user details
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica um usuário por e-mail e senha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos")
    })
    public ResponseEntity<UserResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
