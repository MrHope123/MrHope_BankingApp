package com.MrHope123.MrHope_BankingApp.service.impl;

import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.UserRequest;
import org.springframework.stereotype.Service;


public interface UserService {

    BankResponse createAccount(UserRequest userRequest);
}
