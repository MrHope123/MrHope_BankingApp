package com.MrHope123.MrHope_BankingApp;

import com.MrHope123.MrHope_BankingApp.dto.CreditDebitRequest;
import com.MrHope123.MrHope_BankingApp.service.impl.UserService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the whole application against the in-memory H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
class BankingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    private String createAccount() throws Exception {
        String body = "{\"firstName\":\"Thabo\",\"lastName\":\"Mokoena\","
                + "\"email\":\"" + UUID.randomUUID() + "@example.com\",\"phoneNumber\":\"0710000000\"}";
        String json = mockMvc.perform(post("/api/user").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accountInfo.accountNumber");
    }

    private void postJson(String path, String body, int expectedStatus) throws Exception {
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus));
    }

    private BigDecimal balanceOf(String accountNumber) throws Exception {
        String json = mockMvc.perform(get("/api/user/" + accountNumber + "/balance"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Object balance = JsonPath.read(json, "$.accountInfo.accountBalance");
        return new BigDecimal(balance.toString());
    }

    private String amountBody(String accountNumber, String amount) {
        return "{\"accountNumber\":\"" + accountNumber + "\",\"amount\":" + amount + "}";
    }

    @Test
    void fullFlow_createCreditDebitTransfer() throws Exception {
        String a = createAccount();
        String b = createAccount();

        postJson("/api/user/credit", amountBody(a, "100.00"), 200);
        postJson("/api/user/debit", amountBody(a, "30.00"), 200);
        assertEquals(0, new BigDecimal("70.00").compareTo(balanceOf(a)));

        // overdraw is refused and the balance does not change
        postJson("/api/user/debit", amountBody(a, "500.00"), 422);
        assertEquals(0, new BigDecimal("70.00").compareTo(balanceOf(a)));

        postJson("/api/user/transfer",
                "{\"sourceAccountNumber\":\"" + a + "\",\"destinationAccountNumber\":\"" + b + "\",\"amount\":20.00}", 200);
        assertEquals(0, new BigDecimal("50.00").compareTo(balanceOf(a)));
        assertEquals(0, new BigDecimal("20.00").compareTo(balanceOf(b)));
    }

    @Test
    void duplicateEmail_isRejectedWith409() throws Exception {
        String body = "{\"firstName\":\"Thabo\",\"lastName\":\"Mokoena\","
                + "\"email\":\"dup-" + UUID.randomUUID() + "@example.com\",\"phoneNumber\":\"0710000000\"}";

        postJson("/api/user", body, 201);
        postJson("/api/user", body, 409);
    }

    @Test
    void unknownAccount_returns404() throws Exception {
        mockMvc.perform(get("/api/user/0000000000/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("003"));
    }

    @Test
    void concurrentCredits_doNotLoseUpdates() throws Exception {
        String account = createAccount();
        int threads = 10;
        int creditsPerThread = 5;
        BigDecimal amount = new BigDecimal("10.00");

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                for (int i = 0; i < creditsPerThread; i++) {
                    userService.creditAccount(new CreditDebitRequest(account, amount));
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();

        BigDecimal expected = amount.multiply(BigDecimal.valueOf((long) threads * creditsPerThread));
        assertEquals(0, expected.compareTo(balanceOf(account)));
    }
}
