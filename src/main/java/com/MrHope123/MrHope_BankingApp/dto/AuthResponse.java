package com.MrHope123.MrHope_BankingApp.dto;

/** Returned by login: send the token as "Authorization: Bearer &lt;token&gt;" on every other request. */
public record AuthResponse(String token, String tokenType, long expiresInSeconds, String accountNumber) {
}
