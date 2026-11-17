package com.MrHope123.MrHope_BankingApp.service.impl;

import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.CreditDebitRequest;
import com.MrHope123.MrHope_BankingApp.dto.TransferRequest;
import com.MrHope123.MrHope_BankingApp.dto.UserRequest;

public interface UserService {

    BankResponse createAccount(UserRequest userRequest);

    BankResponse balanceEnquiry(String accountNumber);

    String nameEnquiry(String accountNumber);

    BankResponse creditAccount(CreditDebitRequest request);

    BankResponse debitAccount(CreditDebitRequest request);

    BankResponse transfer(TransferRequest request);
}
