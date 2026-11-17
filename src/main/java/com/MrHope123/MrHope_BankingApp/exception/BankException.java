package com.MrHope123.MrHope_BankingApp.exception;

import com.MrHope123.MrHope_BankingApp.utils.AccountUtils;
import org.springframework.http.HttpStatus;

/**
 * Business-rule failure. Carries the HTTP status and the bank response code
 * so the global handler can turn it into a consistent JSON error.
 */
public class BankException extends RuntimeException {

    private final HttpStatus status;
    private final String responseCode;

    public BankException(HttpStatus status, String responseCode, String message) {
        super(message);
        this.status = status;
        this.responseCode = responseCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public static BankException accountExists() {
        return new BankException(HttpStatus.CONFLICT,
                AccountUtils.ACCOUNT_EXISTS_CODE, AccountUtils.ACCOUNT_EXISTS_MESSAGE);
    }

    public static BankException accountNotFound() {
        return new BankException(HttpStatus.NOT_FOUND,
                AccountUtils.ACCOUNT_NOT_EXIST_CODE, AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE);
    }

    public static BankException insufficientFunds() {
        return new BankException(HttpStatus.UNPROCESSABLE_ENTITY,
                AccountUtils.INSUFFICIENT_FUNDS_CODE, AccountUtils.INSUFFICIENT_FUNDS_MESSAGE);
    }

    public static BankException invalidCredentials() {
        return new BankException(HttpStatus.UNAUTHORIZED,
                AccountUtils.INVALID_CREDENTIALS_CODE, AccountUtils.INVALID_CREDENTIALS_MESSAGE);
    }

    public static BankException accessDenied() {
        return new BankException(HttpStatus.FORBIDDEN,
                AccountUtils.ACCESS_DENIED_CODE, AccountUtils.ACCESS_DENIED_MESSAGE);
    }

    public static BankException sameAccountTransfer() {
        return new BankException(HttpStatus.BAD_REQUEST,
                AccountUtils.SAME_ACCOUNT_TRANSFER_CODE, AccountUtils.SAME_ACCOUNT_TRANSFER_MESSAGE);
    }
}
