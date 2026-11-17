package com.MrHope123.MrHope_BankingApp.service.impl;

import com.MrHope123.MrHope_BankingApp.dto.AuthResponse;
import com.MrHope123.MrHope_BankingApp.dto.LoginRequest;
import com.MrHope123.MrHope_BankingApp.entity.User;
import com.MrHope123.MrHope_BankingApp.exception.BankException;
import com.MrHope123.MrHope_BankingApp.repository.UserRepository;
import com.MrHope123.MrHope_BankingApp.security.TokenService;
import com.MrHope123.MrHope_BankingApp.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private User user() {
        return User.builder()
                .email("thabo@example.com")
                .password("hashed-pw")
                .accountNumber("2026000001")
                .build();
    }

    @Test
    void login_correctPassword_returnsTokenAndAccountNumber() {
        when(userRepository.findByEmail("thabo@example.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("Password123!", "hashed-pw")).thenReturn(true);
        when(tokenService.generateToken("thabo@example.com")).thenReturn("signed.jwt.token");
        when(tokenService.getExpirySeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest("thabo@example.com", "Password123!"));

        assertEquals("signed.jwt.token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresInSeconds());
        assertEquals("2026000001", response.accountNumber());
    }

    @Test
    void login_normalisesEmail() {
        when(userRepository.findByEmail("thabo@example.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("Password123!", "hashed-pw")).thenReturn(true);
        when(tokenService.generateToken("thabo@example.com")).thenReturn("t");

        AuthResponse response = authService.login(new LoginRequest("  THABO@Example.com ", "Password123!"));

        assertEquals("t", response.token());
    }

    @Test
    void login_wrongPassword_isRejectedWithoutIssuingAToken() {
        when(userRepository.findByEmail("thabo@example.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("wrong", "hashed-pw")).thenReturn(false);

        BankException ex = assertThrows(BankException.class,
                () -> authService.login(new LoginRequest("thabo@example.com", "wrong")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(AccountUtils.INVALID_CREDENTIALS_CODE, ex.getResponseCode());
        verifyNoInteractions(tokenService);
    }

    @Test
    void login_unknownEmail_givesTheSameErrorAsAWrongPassword() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        BankException ex = assertThrows(BankException.class,
                () -> authService.login(new LoginRequest("nobody@example.com", "Password123!")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(AccountUtils.INVALID_CREDENTIALS_MESSAGE, ex.getMessage());
        verifyNoInteractions(tokenService);
    }
}
