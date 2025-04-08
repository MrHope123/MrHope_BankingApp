package com.MrHope123.MrHope_BankingApp.service.impl;

import com.MrHope123.MrHope_BankingApp.dto.AccountInfo;
import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.EmailDetails;
import com.MrHope123.MrHope_BankingApp.dto.UserRequest;
import com.MrHope123.MrHope_BankingApp.entity.User;
import com.MrHope123.MrHope_BankingApp.repository.UserRepository;
import com.MrHope123.MrHope_BankingApp.utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class UserServiceImpl implements UserService{

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmailService emailService;

    @Override
    public BankResponse createAccount(UserRequest userRequest) {
        /*
        * Creating an account - saving a new user into the db
        *Check if user already has an account
        */

        if (userRepository.existsByEmail(userRequest.getEmail())){
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_EXISTS_MESSAGE)
                    .accountInfo(null)
                    .build();
        }

        User newUser = User.builder()
                .firstName(userRequest.getFirstName())
                .lastName(userRequest.getLastName())
                .otherName(userRequest.getOtherName())
                .gender(userRequest.getGender())
                .address(userRequest.getAddress())
                .stateOfOrigin(userRequest.getStateOfOrigin())
                .accountNumber(AccountUtils.generateAccountNumber())
                .accountBalance(BigDecimal.ZERO)
                .email(userRequest.getEmail())
                .phoneNumber(userRequest.getPhoneNumber())
                .alternativePhoneNumber(userRequest.getAlternativePhoneNumber())
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.save(newUser);

        EmailDetails emailDetails = EmailDetails.builder()
                .recipient(savedUser.getEmail())
                .subject("ACCOUNT CREATION")
                .messageBody("Congratulations! Your account has been successfully created.\nYour Account Details: \n"+
                        "Account name: "+ savedUser.getFirstName()+" "+ savedUser.getLastName()+" "+ savedUser.getOtherName()+"\nAccount Number: "+savedUser.getAccountNumber())

                .build();

        emailService.sendEmailAlert(emailDetails);

        return BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_CREATION_SUCCESS_MESSAGE)
                .accountInfo(AccountInfo.builder()
                        .AccountBalance(savedUser.getAccountBalance())
                        .AccountNumber(savedUser.getAccountNumber())
                        .AccountName(savedUser.getFirstName()+" "+ savedUser.getLastName()+" "+ savedUser.getOtherName())
                        .build())
                .build();
    }
}