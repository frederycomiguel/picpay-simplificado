package com.picpaysimplificado.service;

import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserRepository;
import com.picpaysimplificado.dto.UserCreateRequest;
import com.picpaysimplificado.dto.UserResponse;
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

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findById(id);
        return toResponse(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + id));
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

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
