package com.MrHope123.MrHope_BankingApp;

import com.MrHope123.MrHope_BankingApp.entity.User;
import com.MrHope123.MrHope_BankingApp.repository.UserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the API is locked down: no token, bad tokens and other customers' accounts are all refused,
 * and refused requests change nothing.
 */
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserRepository userRepository;

    private String tokenWith(JwtEncoder encoder, String subject, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    // ---- who can get in ----

    @Test
    void protectedEndpoints_withoutToken_return401() throws Exception {
        mockMvc.perform(get("/api/user/2026000001/balance"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("013"));
        mockMvc.perform(post("/api/user/credit").contentType(MediaType.APPLICATION_JSON)
                        .content(amountBody("2026000001", "10.00")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/user/debit").contentType(MediaType.APPLICATION_JSON)
                        .content(amountBody("2026000001", "10.00")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/user/transfer").contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody("2026000001", "2026000002", "10.00")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/user/2026000001/balance").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("013"));
    }

    @Test
    void expiredToken_returns401() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        Instant now = Instant.now();
        String expired = tokenWith(jwtEncoder, thabo.email(), now.minus(Duration.ofMinutes(10)), now.minus(Duration.ofMinutes(5)));

        mockMvc.perform(get("/api/user/" + thabo.accountNumber() + "/balance").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKey_returns401() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        JwtEncoder foreignEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherKey, "HmacSHA256")));
        Instant now = Instant.now();
        String forged = tokenWith(foreignEncoder, thabo.email(), now, now.plus(Duration.ofHours(1)));

        mockMvc.perform(get("/api/user/" + thabo.accountNumber() + "/balance").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validToken_works() throws Exception {
        TestCustomer thabo = signUpAndLogIn();

        authedGet(thabo, "/api/user/" + thabo.accountNumber() + "/balance").andExpect(status().isOk());
    }

    // ---- login ----

    @Test
    void login_wrongPassword_returns401() throws Exception {
        TestCustomer thabo = signUpAndLogIn();

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + thabo.email() + "\",\"password\":\"WrongPassword1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("011"));
    }

    @Test
    void login_unknownEmail_looksTheSameAsWrongPassword() throws Exception {
        String unknown = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody-here@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        TestCustomer thabo = signUpAndLogIn();
        String wrongPassword = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + thabo.email() + "\",\"password\":\"WrongPassword1\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertEquals(unknown, wrongPassword);
    }

    @Test
    void passwords_areStoredHashed() throws Exception {
        String email = "hash-" + java.util.UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        User stored = userRepository.findByEmail(email).orElseThrow();

        assertNotEquals(PASSWORD, stored.getPassword());
        assertTrue(stored.getPassword().startsWith("$2"), "expected a BCrypt hash");
    }

    // ---- who can touch which account ----

    @Test
    void customer_cannotReadSomeoneElsesBalanceOrName() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        TestCustomer lerato = signUpAndLogIn();

        authedGet(lerato, "/api/user/" + thabo.accountNumber() + "/balance")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("012"));
        authedGet(lerato, "/api/user/" + thabo.accountNumber() + "/name")
                .andExpect(status().isForbidden());
    }

    @Test
    void customer_cannotDebitOrCreditSomeoneElsesAccount_andNothingChanges() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        TestCustomer lerato = signUpAndLogIn();
        authedPost(thabo, "/api/user/credit", amountBody(thabo.accountNumber(), "100.00")).andExpect(status().isOk());

        authedPost(lerato, "/api/user/debit", amountBody(thabo.accountNumber(), "50.00"))
                .andExpect(status().isForbidden());
        authedPost(lerato, "/api/user/credit", amountBody(thabo.accountNumber(), "50.00"))
                .andExpect(status().isForbidden());

        assertEquals(0, new BigDecimal("100.00").compareTo(balanceOf(thabo, thabo.accountNumber())));
    }

    @Test
    void customer_cannotTransferOutOfSomeoneElsesAccount_andNothingChanges() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        TestCustomer lerato = signUpAndLogIn();
        authedPost(thabo, "/api/user/credit", amountBody(thabo.accountNumber(), "100.00")).andExpect(status().isOk());

        // lerato tries to pull thabo's money into her own account
        authedPost(lerato, "/api/user/transfer", transferBody(thabo.accountNumber(), lerato.accountNumber(), "60.00"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("012"));

        assertEquals(0, new BigDecimal("100.00").compareTo(balanceOf(thabo, thabo.accountNumber())));
        assertEquals(0, BigDecimal.ZERO.compareTo(balanceOf(lerato, lerato.accountNumber())));
    }

    @Test
    void transferFromSomeoneElsesAccount_isForbiddenEvenIfTheDestinationDoesNotExist() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        TestCustomer lerato = signUpAndLogIn();

        // must be 403, not 404, so a stranger cannot use the response to probe account numbers
        authedPost(lerato, "/api/user/transfer", transferBody(thabo.accountNumber(), "0000000000", "10.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customer_canSendMoneyToSomeoneElse_butCannotThenReadTheirAccount() throws Exception {
        TestCustomer thabo = signUpAndLogIn();
        TestCustomer lerato = signUpAndLogIn();
        authedPost(thabo, "/api/user/credit", amountBody(thabo.accountNumber(), "100.00")).andExpect(status().isOk());

        authedPost(thabo, "/api/user/transfer", transferBody(thabo.accountNumber(), lerato.accountNumber(), "25.00"))
                .andExpect(status().isOk());

        assertEquals(0, new BigDecimal("25.00").compareTo(balanceOf(lerato, lerato.accountNumber())));
        authedGet(thabo, "/api/user/" + lerato.accountNumber() + "/balance").andExpect(status().isForbidden());
    }

    // ---- public docs ----

    @Test
    void apiDocs_arePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
