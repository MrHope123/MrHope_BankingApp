package com.MrHope123.MrHope_BankingApp.controller;

import com.MrHope123.MrHope_BankingApp.config.SecurityConfig;
import com.MrHope123.MrHope_BankingApp.dto.AccountInfo;
import com.MrHope123.MrHope_BankingApp.dto.AuthResponse;
import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.exception.BankException;
import com.MrHope123.MrHope_BankingApp.service.impl.AuthService;
import com.MrHope123.MrHope_BankingApp.service.impl.UserService;
import com.MrHope123.MrHope_BankingApp.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String VALID_USER = "{\"firstName\":\"Thabo\",\"lastName\":\"Mokoena\","
            + "\"email\":\"thabo@example.com\",\"phoneNumber\":\"0710000000\",\"password\":\"Password123!\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ---- register (public: no token needed) ----

    @Test
    void register_validRequest_returns201WithoutAToken() throws Exception {
        when(userService.createAccount(any())).thenReturn(BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_CREATION_SUCCESS_MESSAGE)
                .accountInfo(AccountInfo.builder()
                        .AccountName("Thabo Mokoena")
                        .AccountNumber("2026123456")
                        .AccountBalance(BigDecimal.ZERO)
                        .build())
                .build());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE))
                .andExpect(jsonPath("$.accountInfo.accountNumber").value("2026123456"));
    }

    @Test
    void register_responseNeverContainsThePassword() throws Exception {
        when(userService.createAccount(any())).thenReturn(BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_CREATION_SUCCESS_MESSAGE)
                .build());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_missingAndInvalidFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"email\":\"not-an-email\",\"phoneNumber\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.VALIDATION_ERROR_CODE))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phoneNumber").exists())
                .andExpect(jsonPath("$.errors.password").exists());
        verifyNoInteractions(userService);
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER.replace("Password123!", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        verifyNoInteractions(userService);
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(userService.createAccount(any())).thenThrow(BankException.accountExists());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_EXISTS_CODE));
    }

    // ---- login (public) ----

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("signed.jwt.token", "Bearer", 3600, "2026123456"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"thabo@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.accountNumber").value("2026123456"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(BankException.invalidCredentials());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"thabo@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.INVALID_CREDENTIALS_CODE));
    }

    @Test
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
        verifyNoInteractions(authService);
    }
}
