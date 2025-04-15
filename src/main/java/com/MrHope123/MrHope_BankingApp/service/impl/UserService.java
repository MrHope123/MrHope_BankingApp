package com.MrHope123.MrHope_BankingApp.service.impl;

import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.CreditDebitRequest;
import com.MrHope123.MrHope_BankingApp.dto.EnquiryRequest;
import com.MrHope123.MrHope_BankingApp.dto.UserRequest;
import org.springframework.stereotype.Service;


public interface UserService {

    BankResponse createAccount(UserRequest userRequest);

    BankResponse balanceEnquiry(EnquiryRequest enquiryRequest);

    String nameEnquiry(EnquiryRequest enquiryRequest);

    BankResponse creditDebitAccount(CreditDebitRequest creditDebitRequest);
}
