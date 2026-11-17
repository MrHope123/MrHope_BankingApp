package com.MrHope123.MrHope_BankingApp.config;

import com.MrHope123.MrHope_BankingApp.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final JwtConfig config = new JwtConfig();

    @Test
    void secretShorterThan32Characters_failsFast() {
        assertThrows(IllegalStateException.class, () -> config.jwtSecretKey("too-short"));
    }

    @Test
    void blankSecret_generatesARandomKeyEachTime() {
        SecretKey first = config.jwtSecretKey("");
        SecretKey second = config.jwtSecretKey("  ");

        assertEquals(32, first.getEncoded().length);
        assertFalse(java.util.Arrays.equals(first.getEncoded(), second.getEncoded()));
    }

    @Test
    void generatedToken_canBeVerifiedAndCarriesTheSubjectAndExpiry() {
        SecretKey key = config.jwtSecretKey(SECRET);
        TokenService tokenService = new TokenService(config.jwtEncoder(key), 60);

        Jwt jwt = config.jwtDecoder(key).decode(tokenService.generateToken("thabo@example.com"));

        assertEquals("thabo@example.com", jwt.getSubject());
        assertNotNull(jwt.getExpiresAt());
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));
        assertEquals(3600L, tokenService.getExpirySeconds());
    }

    @Test
    void tokenSignedWithADifferentKey_isRejected() {
        SecretKey signingKey = config.jwtSecretKey("ffffffffffffffffffffffffffffffff");
        SecretKey verifyingKey = config.jwtSecretKey(SECRET);
        String token = new TokenService(config.jwtEncoder(signingKey), 60).generateToken("thabo@example.com");

        assertThrows(JwtException.class, () -> config.jwtDecoder(verifyingKey).decode(token));
    }

    @Test
    void tamperedToken_isRejected() {
        SecretKey key = config.jwtSecretKey(SECRET);
        String token = new TokenService(config.jwtEncoder(key), 60).generateToken("thabo@example.com");
        String[] parts = token.split("\\.");
        // Swap in a payload for a different user but keep the original signature.
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"attacker@example.com\",\"exp\":9999999999}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThrows(JwtException.class, () -> config.jwtDecoder(key).decode(forged));
    }
}
