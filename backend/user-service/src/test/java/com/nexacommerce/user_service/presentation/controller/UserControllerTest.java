package com.nexacommerce.user_service.presentation.controller;

import com.nexacommerce.user_service.application.dto.UserRegistrationRequest;
import com.nexacommerce.user_service.application.dto.UserResponse;
import com.nexacommerce.user_service.application.dto.UserUpdateRequest;
import com.nexacommerce.user_service.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

        sampleResponse = new UserResponse(
                1L,
                "John",
                "Doe",
                "john.doe@example.com",
                "CUSTOMER",
                true,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/users/register - Should return 201 Created with valid request")
    void register_Returns201Created_WhenValidRequest() throws Exception {
        when(userService.register(any(UserRegistrationRequest.class))).thenReturn(sampleResponse);

        String jsonRequest = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com",
                    "password": "Password123"
                }
                """;

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.active", is(true)));

        verify(userService).register(any(UserRegistrationRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should return 200 OK when user exists")
    void getById_Returns200Ok_WhenUserExists() throws Exception {
        when(userService.getById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")));

        verify(userService).getById(1L);
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Should return 200 OK on profile update")
    void updateProfile_Returns200Ok_WhenValidRequest() throws Exception {
        UserResponse updatedResponse = new UserResponse(
                1L, "Johnny", "Smith", "john.doe@example.com", "CUSTOMER", true, LocalDateTime.now()
        );

        when(userService.updateProfile(eq(1L), any(UserUpdateRequest.class))).thenReturn(updatedResponse);

        String jsonRequest = """
                {
                    "firstName": "Johnny",
                    "lastName": "Smith"
                }
                """;

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Johnny")))
                .andExpect(jsonPath("$.lastName", is("Smith")));

        verify(userService).updateProfile(eq(1L), any(UserUpdateRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/users - Should return 200 OK with list of users")
    void getAll_Returns200Ok_WithUserList() throws Exception {
        when(userService.getAll()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", is("john.doe@example.com")));

        verify(userService).getAll();
    }
}
