package com.MrHope123.MrHope_BankingApp.controller;

import com.MrHope123.MrHope_BankingApp.dto.AccountInfo;
import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void createAccount_returnsServiceResponseAsJson() throws Exception {
        BankResponse response = BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_CREATION_SUCCESS_MESSAGE)
                .accountInfo(AccountInfo.builder()
                        .AccountName("Thabo Mokoena J")
                        .AccountNumber("2026123456")
                        .AccountBalance(BigDecimal.ZERO)
                        .build())
                .build();
        when(userService.createAccount(any())).thenReturn(response);

        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Thabo\",\"lastName\":\"Mokoena\",\"email\":\"thabo@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE));
    }

    @Test
    void credit_returnsServiceResponseAsJson() throws Exception {
        BankResponse response = BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_CREDIT_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_CREDIT_SUCCESS_MESSAGE)
                .build();
        when(userService.creditDebitAccount(any())).thenReturn(response);

        mockMvc.perform(post("/api/user/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"2026123456\",\"amount\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(AccountUtils.ACCOUNT_CREDIT_SUCCESS_CODE));
    }
}
