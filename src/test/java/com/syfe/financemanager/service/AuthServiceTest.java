package com.syfe.financemanager.service;

import com.syfe.financemanager.common.ConflictException;
import com.syfe.financemanager.common.ResourceNotFoundException;
import com.syfe.financemanager.dto.auth.LoginRequest;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.auth.RegisterRequest;
import com.syfe.financemanager.dto.auth.RegisterResponse;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("test@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .phoneNumber("+1234567890")
                .build();
    }

    @Test
    void testRegister_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("test@example.com")
                .password("password123")
                .fullName("Test User")
                .phoneNumber("+1234567890")
                .build();

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        RegisterResponse response = authService.register(request);

        assertThat(response.getMessage()).isEqualTo("User registered successfully");
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("test@example.com")
                .password("password123")
                .fullName("Test User")
                .phoneNumber("+1234567890")
                .build();

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("test@example.com")
                .password("password123")
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        MessageResponse response = authService.login(request, httpRequest, httpResponse);

        assertThat(response.getMessage()).isEqualTo("Login successful");
        verify(securityContextRepository).saveContext(any(SecurityContext.class), eq(httpRequest), eq(httpResponse));
    }

    @Test
    void testLogout_Success() {
        when(httpRequest.getSession(false)).thenReturn(session);

        MessageResponse response = authService.logout(httpRequest, httpResponse);

        assertThat(response.getMessage()).isEqualTo("Logout successful");
        verify(session).invalidate();
        verify(httpResponse).addCookie(any());
    }

    @Test
    void testGetCurrentAuthenticatedUser_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("test@example.com");
        when(auth.getName()).thenReturn("test@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));

        User result = authService.getCurrentAuthenticatedUser();

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("test@example.com");
    }

    @Test
    void testGetCurrentAuthenticatedUser_NotAuthenticated() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> authService.getCurrentAuthenticatedUser())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
