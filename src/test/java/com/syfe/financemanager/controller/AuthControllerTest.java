package com.syfe.financemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.financemanager.dto.auth.LoginRequest;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.auth.RegisterRequest;
import com.syfe.financemanager.dto.auth.RegisterResponse;
import com.syfe.financemanager.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void testRegister_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("user@example.com")
                .password("password123")
                .fullName("John Doe")
                .phoneNumber("+1234567890")
                .build();

        RegisterResponse response = RegisterResponse.builder()
                .message("User registered successfully")
                .userId(1L)
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void testRegister_ValidationError() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("not-an-email")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("user@example.com")
                .password("password123")
                .build();

        when(authService.login(any(), any(), any()))
                .thenReturn(new MessageResponse("Login successful"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @WithMockUser
    void testLogout_Success() throws Exception {
        when(authService.logout(any(), any()))
                .thenReturn(new MessageResponse("Logout successful"));

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }
}
