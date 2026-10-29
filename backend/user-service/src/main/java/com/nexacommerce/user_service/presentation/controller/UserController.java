package com.nexacommerce.user_service.presentation.controller;

import com.nexacommerce.user_service.application.dto.LoginRequest;
import com.nexacommerce.user_service.application.dto.LoginResponse;
import com.nexacommerce.user_service.application.dto.UserRegistrationRequest;
import com.nexacommerce.user_service.application.dto.UserResponse;
import com.nexacommerce.user_service.application.dto.UserUpdateRequest;
import com.nexacommerce.user_service.application.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody UserRegistrationRequest request
    ) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return userService.login(request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return userService.updateProfile(id, request);
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll();
    }
}
