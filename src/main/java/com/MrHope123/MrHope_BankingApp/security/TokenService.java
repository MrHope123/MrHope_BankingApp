package com.MrHope123.MrHope_BankingApp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private static final String ISSUER = "mrhope-banking-app";

    private final JwtEncoder jwtEncoder;
    private final Duration expiry;

    public TokenService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiry-minutes:60}") long expiryMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expiry = Duration.ofMinutes(expiryMinutes);
    }

    /** Creates a signed token whose subject is the customer's email. */
    public String generateToken(String subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(expiry))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirySeconds() {
        return expiry.toSeconds();
    }
}
