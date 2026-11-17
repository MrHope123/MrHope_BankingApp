package com.MrHope123.MrHope_BankingApp;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Starts the whole application on the in-memory H2 database and offers helpers to act as real customers. */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

    static final String PASSWORD = "Password123!";

    @Autowired
    protected MockMvc mockMvc;

    /** A customer who has signed up and logged in. */
    record TestCustomer(String email, String accountNumber, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    protected String registerBody(String email) {
        return "{\"firstName\":\"Thabo\",\"lastName\":\"Mokoena\",\"email\":\"" + email
                + "\",\"phoneNumber\":\"0710000000\",\"password\":\"" + PASSWORD + "\"}";
    }

    /** Signs up a brand-new customer through the public endpoint, then logs in. */
    protected TestCustomer signUpAndLogIn() throws Exception {
        String email = UUID.randomUUID() + "@example.com";

        String registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String accountNumber = JsonPath.read(registered, "$.accountInfo.accountNumber");

        String loggedIn = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loggedIn, "$.token");

        return new TestCustomer(email, accountNumber, token);
    }

    protected ResultActions authedPost(TestCustomer who, String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .header("Authorization", who.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    protected ResultActions authedGet(TestCustomer who, String path) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", who.bearer()));
    }

    protected String amountBody(String accountNumber, String amount) {
        return "{\"accountNumber\":\"" + accountNumber + "\",\"amount\":" + amount + "}";
    }

    protected String transferBody(String from, String to, String amount) {
        return "{\"sourceAccountNumber\":\"" + from + "\",\"destinationAccountNumber\":\"" + to
                + "\",\"amount\":" + amount + "}";
    }

    /** Reads the balance of an account through the API, as the given customer. */
    protected BigDecimal balanceOf(TestCustomer who, String accountNumber) throws Exception {
        String json = authedGet(who, "/api/user/" + accountNumber + "/balance")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Object balance = JsonPath.read(json, "$.accountInfo.accountBalance");
        return new BigDecimal(balance.toString());
    }
}
