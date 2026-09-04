package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.auth.LoginRequest;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.auth.RegisterRequest;
import com.syfe.financemanager.dto.auth.RegisterResponse;
import com.syfe.financemanager.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    MessageResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    MessageResponse logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    User getCurrentAuthenticatedUser();
}
