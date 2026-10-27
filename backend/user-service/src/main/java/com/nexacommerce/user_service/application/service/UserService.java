package com.nexacommerce.user_service.application.service;

import com.nexacommerce.user_service.application.dto.UserRegistrationRequest;
import com.nexacommerce.user_service.application.dto.UserResponse;
import com.nexacommerce.user_service.application.dto.UserUpdateRequest;
import com.nexacommerce.user_service.domain.model.User;
import com.nexacommerce.user_service.infrastructure.persistence.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordHash
        );

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public UserResponse registerUser(UserRegistrationRequest request) {
        return register(request);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = findUser(id);
        return toResponse(user);
    }

    public UserResponse getByIdUser(Long id) {
        return getById(id);
    }

    @Transactional
    public UserResponse updateProfile(
            Long id,
            UserUpdateRequest request
    ) {
        User user = findUser(id);

        user.updateProfile(
                request.firstName().trim(),
                request.lastName().trim()
        );

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User findUser(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "User ID must be positive"
            );
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with ID: " + id
                ));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
