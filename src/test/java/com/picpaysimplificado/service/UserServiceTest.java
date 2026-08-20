package com.picpaysimplificado.service;

import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserRepository;
import com.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.dto.UserCreateRequest;
import com.picpaysimplificado.dto.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .firstName("Carlos")
                .lastName("Souza")
                .document("11122233344")
                .email("carlos@email.com")
                .password("secret123")
                .balance(new BigDecimal("500.00"))
                .userType(UserType.COMMON)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully when data is valid")
        void shouldCreateUserSuccessfully() {
            UserCreateRequest request = new UserCreateRequest(
                    "Carlos", "Souza", "11122233344", "carlos@email.com",
                    "secret123", new BigDecimal("500.00"), UserType.COMMON
            );

            when(userRepository.existsByDocument(request.document())).thenReturn(false);
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);

            UserResponse response = userService.createUser(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.firstName()).isEqualTo("Carlos");
            assertThat(response.document()).isEqualTo("11122233344");
            assertThat(response.balance()).isEqualByComparingTo("500.00");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when document already exists")
        void shouldThrowExceptionWhenDocumentExists() {
            UserCreateRequest request = new UserCreateRequest(
                    "Carlos", "Souza", "11122233344", "carlos@email.com",
                    "secret123", new BigDecimal("500.00"), UserType.COMMON
            );

            when(userRepository.existsByDocument(request.document())).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Já existe um usuário com este documento");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            UserCreateRequest request = new UserCreateRequest(
                    "Carlos", "Souza", "11122233344", "carlos@email.com",
                    "secret123", new BigDecimal("500.00"), UserType.COMMON
            );

            when(userRepository.existsByDocument(request.document())).thenReturn(false);
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Já existe um usuário com este e-mail");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Find User Tests")
    class FindUserTests {

        @Test
        @DisplayName("Should return user when ID exists")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            User result = userService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when user is not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Usuário não encontrado com ID: 99");
        }

        @Test
        @DisplayName("Should return list of all users")
        void shouldReturnAllUsers() {
            when(userRepository.findAll()).thenReturn(List.of(sampleUser));

            List<UserResponse> list = userService.getAllUsers();

            assertThat(list).hasSize(1);
            assertThat(list.get(0).firstName()).isEqualTo("Carlos");
        }
    }
}
