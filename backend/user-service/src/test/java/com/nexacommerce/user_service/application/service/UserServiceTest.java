package com.nexacommerce.user_service.application.service;

import com.nexacommerce.user_service.application.dto.UserRegistrationRequest;
import com.nexacommerce.user_service.application.dto.UserResponse;
import com.nexacommerce.user_service.application.dto.UserUpdateRequest;
import com.nexacommerce.user_service.domain.model.User;
import com.nexacommerce.user_service.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("John", "Doe", "john.doe@example.com", "encodedPassword123");
        ReflectionTestUtils.setField(sampleUser, "id", 1L);
    }

    @Nested
    @DisplayName("register() Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should successfully register a new user when email is unique")
        void register_Success() {
            // Arrange
            UserRegistrationRequest request = new UserRegistrationRequest(
                    "John", "Doe", "john.doe@example.com", "Password@123"
            );

            when(userRepository.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword123");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User userToSave = invocation.getArgument(0);
                ReflectionTestUtils.setField(userToSave, "id", 1L);
                return userToSave;
            });

            // Act
            UserResponse response = userService.register(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.firstName()).isEqualTo("John");
            assertThat(response.lastName()).isEqualTo("Doe");
            assertThat(response.email()).isEqualTo("john.doe@example.com");
            assertThat(response.role()).isEqualTo("CUSTOMER");
            assertThat(response.active()).isTrue();

            verify(userRepository, times(1)).existsByEmailIgnoreCase("john.doe@example.com");
            verify(passwordEncoder, times(1)).encode("Password@123");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email already exists")
        void register_ThrowsException_WhenEmailAlreadyExists() {
            // Arrange
            UserRegistrationRequest request = new UserRegistrationRequest(
                    "Jane", "Doe", "existing@example.com", "Password@123"
            );

            when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("An account with this email already exists");

            verify(userRepository, times(1)).existsByEmailIgnoreCase("existing@example.com");
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("getById() Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return user response when user exists")
        void getById_Success() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            // Act
            UserResponse response = userService.getById(1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("john.doe@example.com");
            assertThat(response.firstName()).isEqualTo("John");
            assertThat(response.lastName()).isEqualTo("Doe");

            verify(userRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user does not exist")
        void getById_ThrowsException_WhenUserNotFound() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found with ID: 999");

            verify(userRepository, times(1)).findById(999L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user ID is null or negative")
        void getById_ThrowsException_WhenIdIsInvalid() {
            assertThatThrownBy(() -> userService.getById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID must be positive");

            assertThatThrownBy(() -> userService.getById(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID must be positive");

            assertThatThrownBy(() -> userService.getById(-5L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID must be positive");

            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("updateProfile() Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should successfully update user first and last name")
        void updateProfile_Success() {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest("Johnny", "Smith");
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);

            // Act
            UserResponse response = userService.updateProfile(1L, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.firstName()).isEqualTo("Johnny");
            assertThat(response.lastName()).isEqualTo("Smith");

            verify(userRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when updating non-existent user")
        void updateProfile_ThrowsException_WhenUserNotFound() {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest("Johnny", "Smith");
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile(99L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found with ID: 99");

            verify(userRepository, times(1)).findById(99L);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("getAll() Tests")
    class GetAllTests {

        @Test
        @DisplayName("Should return list of users when users exist")
        void getAll_Success() {
            // Arrange
            User secondUser = new User("Alice", "Brown", "alice@example.com", "pass456");
            ReflectionTestUtils.setField(secondUser, "id", 2L);

            when(userRepository.findAll()).thenReturn(List.of(sampleUser, secondUser));

            // Act
            List<UserResponse> result = userService.getAll();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).email()).isEqualTo("john.doe@example.com");
            assertThat(result.get(1).email()).isEqualTo("alice@example.com");

            verify(userRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void getAll_ReturnsEmptyList_WhenNoUsers() {
            // Arrange
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<UserResponse> result = userService.getAll();

            // Assert
            assertThat(result).isNotNull().isEmpty();

            verify(userRepository, times(1)).findAll();
        }
    }
}
