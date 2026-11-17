package com.MrHope123.MrHope_BankingApp.controller;

import com.MrHope123.MrHope_BankingApp.config.SecurityConfig;
import com.MrHope123.MrHope_BankingApp.dto.AccountInfo;
import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.exception.BankException;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String CALLER = "thabo@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    /** Needed by the security config; the jwt() helper below skips real token parsing. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor loggedInAsCaller() {
        return jwt().jwt(token -> token.subject(CALLER));
    }

    private BankResponse okResponse(String code, String message) {
        return BankResponse.builder()
                .responseCode(code)
                .responseMessage(message)
                .accountInfo(AccountInfo.builder()
                        .AccountName("Thabo Mokoena")
                        .AccountNumber("2026123456")
                        .AccountBalance(BigDecimal.ZERO)
                        .build())
                .build();
    }

    // ---- authentication ----

    @Test
    void anyEndpoint_withoutToken_returns401InTheStandardErrorShape() throws Exception {
        mockMvc.perform(get("/api/user/2026123456/balance"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.UNAUTHENTICATED_CODE));
        verifyNoInteractions(userService);
    }

    @Test
    void postEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/user/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":50.00}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    // ---- enquiries ----

    @Test
    void balance_passesTheTokenSubjectToTheService() throws Exception {
        when(userService.balanceEnquiry(CALLER, "2026123456"))
                .thenReturn(okResponse(AccountUtils.ACCOUNT_FOUND_CODE, AccountUtils.ACCOUNT_FOUND_MESSAGE));

        mockMvc.perform(get("/api/user/2026123456/balance").with(loggedInAsCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountInfo.accountNumber").value("2026123456"));
    }

    @Test
    void balance_unknownAccount_returns404() throws Exception {
        when(userService.balanceEnquiry(CALLER, "2026999999")).thenThrow(BankException.accountNotFound());

        mockMvc.perform(get("/api/user/2026999999/balance").with(loggedInAsCaller()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_NOT_EXIST_CODE));
    }

    @Test
    void balance_someoneElsesAccount_returns403() throws Exception {
        when(userService.balanceEnquiry(CALLER, "2026000009")).thenThrow(BankException.accessDenied());

        mockMvc.perform(get("/api/user/2026000009/balance").with(loggedInAsCaller()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCESS_DENIED_CODE));
    }

    @Test
    void name_ownAccount_returnsFullName() throws Exception {
        when(userService.nameEnquiry(CALLER, "2026123456")).thenReturn("Thabo Mokoena");

        mockMvc.perform(get("/api/user/2026123456/name").with(loggedInAsCaller()))
                .andExpect(status().isOk())
                .andExpect(content().string("Thabo Mokoena"));
    }

    // ---- credit / debit / transfer ----

    @Test
    void credit_validRequest_returns200() throws Exception {
        when(userService.creditAccount(eq(CALLER), any()))
                .thenReturn(okResponse(AccountUtils.ACCOUNT_CREDIT_SUCCESS_CODE, AccountUtils.ACCOUNT_CREDIT_SUCCESS_MESSAGE));

        mockMvc.perform(post("/api/user/credit")
                        .with(loggedInAsCaller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_CREDIT_SUCCESS_CODE));
    }

    @Test
    void credit_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/user/credit")
                        .with(loggedInAsCaller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
        verifyNoInteractions(userService);
    }

    @Test
    void credit_tooManyDecimalPlaces_returns400() throws Exception {
        mockMvc.perform(post("/api/user/credit")
                        .with(loggedInAsCaller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":1.234}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void credit_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/user/credit")
                        .with(loggedInAsCaller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.VALIDATION_ERROR_CODE));
    }

    @Test
    void debit_badAccountNumber_returns400() throws Exception {
        mockMvc.perform(post("/api/user/debit")
                        .with(loggedInAsCaller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"12\",\"amount\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.accountNumber").exists());
    }

    @Test
    void debit_insufficientFunds_returns422() throws Exception {
        when(userService.debitAccount(eq(CALLER), any())).thenThrow(BankException.insufficientFunds());

        mockMvc.perform(post("/api/user/debit")
                        .with(loggedInAsCaller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":999}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.INSUFFICIENT_FUNDS_CODE));
    }

    @Test
    void debit_someoneElsesAccount_returns403() throws Exception {
        when(userService.debitAccount(eq(CALLER), any())).thenThrow(BankException.accessDenied());

        mockMvc.perform(post("/api/user/debit")
                        .with(loggedInAsCaller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026000009\",\"amount\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_validRequest_returns200() throws Exception {
        when(userService.transfer(eq(CALLER), any()))
                .thenReturn(okResponse(AccountUtils.TRANSFER_SUCCESS_CODE, AccountUtils.TRANSFER_SUCCESS_MESSAGE));

        mockMvc.perform(post("/api/user/transfer")
                        .with(loggedInAsCaller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountNumber\":\"2026000001\",\"destinationAccountNumber\":\"2026000002\",\"amount\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.TRANSFER_SUCCESS_CODE));
    }

    @Test
    void transfer_missingDestination_returns400() throws Exception {
        mockMvc.perform(post("/api/user/transfer")
                        .with(loggedInAsCaller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountNumber\":\"2026000001\",\"amount\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.destinationAccountNumber").exists());
    }
}
