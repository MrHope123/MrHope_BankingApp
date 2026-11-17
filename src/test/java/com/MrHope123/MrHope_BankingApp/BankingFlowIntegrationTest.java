package com.MrHope123.MrHope_BankingApp;

import com.MrHope123.MrHope_BankingApp.dto.CreditDebitRequest;
import com.MrHope123.MrHope_BankingApp.service.impl.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The normal, allowed things a customer can do. Access control is tested in SecurityIntegrationTest. */
class BankingFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void fullFlow_creditDebitAndTransferBetweenTwoCustomers() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        TestCustomer lerato = signUpAndLogIn();

        authedPost(thabo, "/api/user/credit", amountBody(thabo.accountNumber(), "100.00")).andExpect(status().isOk());
        authedPost(thabo, "/api/user/debit", amountBody(thabo.accountNumber(), "30.00")).andExpect(status().isOk());
        assertEquals(0, new BigDecimal("70.00").compareTo(balanceOf(thabo, thabo.accountNumber())));

        // overdraw is refused and the balance does not change
        authedPost(thabo, "/api/user/debit", amountBody(thabo.accountNumber(), "500.00"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(0, new BigDecimal("70.00").compareTo(balanceOf(thabo, thabo.accountNumber())));

        // sending to another customer is allowed; each customer then sees only their own balance
        authedPost(thabo, "/api/user/transfer", transferBody(thabo.accountNumber(), lerato.accountNumber(), "20.00"))
                .andExpect(status().isOk());
        assertEquals(0, new BigDecimal("50.00").compareTo(balanceOf(thabo, thabo.accountNumber())));
        assertEquals(0, new BigDecimal("20.00").compareTo(balanceOf(lerato, lerato.accountNumber())));
    }

    @Test
    void nameEnquiry_ownAccount_returnsTheName() throws Exception {
        TestCustomer thabo = signUpAndLogIn();

        authedGet(thabo, "/api/user/" + thabo.accountNumber() + "/name")
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string("Thabo Mokoena"));
    }

    @Test
    void duplicateEmail_isRejectedWith409() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody(email)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void emailIsCaseInsensitive_forSignUpAndLogin() throws Exception {
        String email = "case-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email.toUpperCase())))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownAccount_returns404() throws Exception {
        TestCustomer thabo = signUpAndLogIn();

        authedGet(thabo, "/api/user/0000000000/balance")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("003"));
    }

    @Test
    void transferToAnUnknownAccount_returns404_andKeepsTheMoney() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        authedPost(thabo, "/api/user/credit", amountBody(thabo.accountNumber(), "40.00")).andExpect(status().isOk());

        authedPost(thabo, "/api/user/transfer", transferBody(thabo.accountNumber(), "0000000000", "10.00"))
                .andExpect(status().isNotFound());
        assertEquals(0, new BigDecimal("40.00").compareTo(balanceOf(thabo, thabo.accountNumber())));
    }

    @Test
    void concurrentCredits_doNotLoseUpdates() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        int threads = 10;
        int creditsPerThread = 5;
        BigDecimal amount = new BigDecimal("10.00");

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                for (int i = 0; i < creditsPerThread; i++) {
                    userService.creditAccount(thabo.email(), new CreditDebitRequest(thabo.accountNumber(), amount));
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();

        BigDecimal expected = amount.multiply(BigDecimal.valueOf((long) threads * creditsPerThread));
        assertEquals(0, expected.compareTo(balanceOf(thabo, thabo.accountNumber())));
    }
}
