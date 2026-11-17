package com.MrHope123.MrHope_BankingApp.service.impl;

import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.CreditDebitRequest;
import com.MrHope123.MrHope_BankingApp.dto.TransferRequest;
import com.MrHope123.MrHope_BankingApp.dto.UserRequest;

/**
 * Every operation on an existing account takes the caller's email (from the verified token)
 * and throws a 403 BankException unless the account belongs to that caller.
 */
public interface UserService {

    /** Sign-up: creates the customer and their account. */
    BankResponse createAccount(UserRequest userRequest);

    BankResponse balanceEnquiry(String callerEmail, String accountNumber);

    String nameEnquiry(String callerEmail, String accountNumber);

    BankResponse creditAccount(String callerEmail, CreditDebitRequest request);

    BankResponse debitAccount(String callerEmail, CreditDebitRequest request);

    /** The source account must belong to the caller; the destination can be any account. */
    BankResponse transfer(String callerEmail, TransferRequest request);
}
