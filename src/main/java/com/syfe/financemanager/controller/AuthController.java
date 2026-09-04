package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.auth.LoginRequest;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.auth.RegisterRequest;
import com.syfe.financemanager.dto.auth.RegisterResponse;
import com.syfe.financemanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, and session-based logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and establish session cookie")
    public ResponseEntity<MessageResponse> login(@Valid @RequestBody LoginRequest request,
                                                 HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {
        MessageResponse response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate session cookie")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        MessageResponse response = authService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }
}
