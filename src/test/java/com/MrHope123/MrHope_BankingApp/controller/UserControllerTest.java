package com.MrHope123.MrHope_BankingApp.controller;

import com.MrHope123.MrHope_BankingApp.dto.AccountInfo;
import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.exception.BankException;
import com.MrHope123.MrHope_BankingApp.service.impl.UserService;
import com.MrHope123.MrHope_BankingApp.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String VALID_USER = "{\"firstName\":\"Thabo\",\"lastName\":\"Mokoena\","
            + "\"email\":\"thabo@example.com\",\"phoneNumber\":\"0710000000\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

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

    // ---- create ----

    @Test
    void createAccount_validRequest_returns201() throws Exception {
        when(userService.createAccount(any()))
                .thenReturn(okResponse(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE, AccountUtils.ACCOUNT_CREATION_SUCCESS_MESSAGE));

        mockMvc.perform(post("/api/user").contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE));
    }

    @Test
    void createAccount_missingAndInvalidFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"email\":\"not-an-email\",\"phoneNumber\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.VALIDATION_ERROR_CODE))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phoneNumber").exists());
        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_duplicate_returns409() throws Exception {
        when(userService.createAccount(any())).thenThrow(BankException.accountExists());

        mockMvc.perform(post("/api/user").contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_EXISTS_CODE));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/user").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.VALIDATION_ERROR_CODE));
    }

    // ---- enquiries ----

    @Test
    void balance_existingAccount_returns200() throws Exception {
        when(userService.balanceEnquiry("2026123456"))
                .thenReturn(okResponse(AccountUtils.ACCOUNT_FOUND_CODE, AccountUtils.ACCOUNT_FOUND_MESSAGE));

        mockMvc.perform(get("/api/user/2026123456/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountInfo.accountNumber").value("2026123456"));
    }

    @Test
    void balance_unknownAccount_returns404() throws Exception {
        when(userService.balanceEnquiry("2026999999")).thenThrow(BankException.accountNotFound());

        mockMvc.perform(get("/api/user/2026999999/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_NOT_EXIST_CODE));
    }

    @Test
    void name_existingAccount_returnsFullName() throws Exception {
        when(userService.nameEnquiry("2026123456")).thenReturn("Thabo Mokoena");

        mockMvc.perform(get("/api/user/2026123456/name"))
                .andExpect(status().isOk())
                .andExpect(content().string("Thabo Mokoena"));
    }

    // ---- credit / debit / transfer ----

    @Test
    void credit_validRequest_returns200() throws Exception {
        when(userService.creditAccount(any()))
                .thenReturn(okResponse(AccountUtils.ACCOUNT_CREDIT_SUCCESS_CODE, AccountUtils.ACCOUNT_CREDIT_SUCCESS_MESSAGE));

        mockMvc.perform(post("/api/user/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_CREDIT_SUCCESS_CODE));
    }

    @Test
    void credit_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/user/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
        verifyNoInteractions(userService);
    }

    @Test
    void credit_tooManyDecimalPlaces_returns400() throws Exception {
        mockMvc.perform(post("/api/user/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":1.234}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void debit_badAccountNumber_returns400() throws Exception {
        mockMvc.perform(post("/api/user/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"12\",\"amount\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.accountNumber").exists());
    }

    @Test
    void debit_insufficientFunds_returns422() throws Exception {
        when(userService.debitAccount(any())).thenThrow(BankException.insufficientFunds());

        mockMvc.perform(post("/api/user/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":999}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.INSUFFICIENT_FUNDS_CODE));
    }

    @Test
    void transfer_validRequest_returns200() throws Exception {
        when(userService.transfer(any()))
                .thenReturn(okResponse(AccountUtils.TRANSFER_SUCCESS_CODE, AccountUtils.TRANSFER_SUCCESS_MESSAGE));

        mockMvc.perform(post("/api/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountNumber\":\"2026000001\",\"destinationAccountNumber\":\"2026000002\",\"amount\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.TRANSFER_SUCCESS_CODE));
    }

    @Test
    void transfer_missingDestination_returns400() throws Exception {
        mockMvc.perform(post("/api/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountNumber\":\"2026000001\",\"amount\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.destinationAccountNumber").exists());
    }
}
