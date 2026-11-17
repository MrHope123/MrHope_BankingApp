package com.MrHope123.MrHope_BankingApp.service.impl;

import com.MrHope123.MrHope_BankingApp.dto.*;
import com.MrHope123.MrHope_BankingApp.entity.User;
import com.MrHope123.MrHope_BankingApp.exception.BankException;
import com.MrHope123.MrHope_BankingApp.repository.UserRepository;
import com.MrHope123.MrHope_BankingApp.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public BankResponse createAccount(UserRequest userRequest) {
        String email = userRequest.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw BankException.accountExists();
        }

        User newUser = User.builder()
                .firstName(userRequest.getFirstName())
                .lastName(userRequest.getLastName())
                .otherName(userRequest.getOtherName())
                .gender(userRequest.getGender())
                .address(userRequest.getAddress())
                .stateOfOrigin(userRequest.getStateOfOrigin())
                .accountNumber(generateUniqueAccountNumber())
                .accountBalance(BigDecimal.ZERO)
                .email(email)
                .phoneNumber(userRequest.getPhoneNumber())
                .alternativePhoneNumber(userRequest.getAlternativePhoneNumber())
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.save(newUser);

        return accountResponse(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE,
                AccountUtils.ACCOUNT_CREATION_SUCCESS_MESSAGE, savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public BankResponse balanceEnquiry(String accountNumber) {
        User foundUser = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(BankException::accountNotFound);

        return accountResponse(AccountUtils.ACCOUNT_FOUND_CODE, AccountUtils.ACCOUNT_FOUND_MESSAGE, foundUser);
    }

    @Override
    @Transactional(readOnly = true)
    public String nameEnquiry(String accountNumber) {
        User foundUser = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(BankException::accountNotFound);

        return fullName(foundUser);
    }

    @Override
    @Transactional
    public BankResponse creditAccount(CreditDebitRequest request) {
        User user = lockAccount(request.getAccountNumber());

        user.setAccountBalance(user.getAccountBalance().add(request.getAmount()));
        userRepository.save(user);

        return accountResponse(AccountUtils.ACCOUNT_CREDIT_SUCCESS_CODE,
                AccountUtils.ACCOUNT_CREDIT_SUCCESS_MESSAGE, user);
    }

    @Override
    @Transactional
    public BankResponse debitAccount(CreditDebitRequest request) {
        User user = lockAccount(request.getAccountNumber());

        if (user.getAccountBalance().compareTo(request.getAmount()) < 0) {
            throw BankException.insufficientFunds();
        }

        user.setAccountBalance(user.getAccountBalance().subtract(request.getAmount()));
        userRepository.save(user);

        return accountResponse(AccountUtils.ACCOUNT_DEBIT_SUCCESS_CODE,
                AccountUtils.ACCOUNT_DEBIT_SUCCESS_MESSAGE, user);
    }

    @Override
    @Transactional
    public BankResponse transfer(TransferRequest request) {
        String sourceNumber = request.getSourceAccountNumber();
        String destinationNumber = request.getDestinationAccountNumber();

        if (sourceNumber.equals(destinationNumber)) {
            throw BankException.sameAccountTransfer();
        }

        // Always lock in the same order so two opposite transfers cannot deadlock.
        boolean sourceFirst = sourceNumber.compareTo(destinationNumber) < 0;
        User first = lockAccount(sourceFirst ? sourceNumber : destinationNumber);
        User second = lockAccount(sourceFirst ? destinationNumber : sourceNumber);
        User source = sourceFirst ? first : second;
        User destination = sourceFirst ? second : first;

        if (source.getAccountBalance().compareTo(request.getAmount()) < 0) {
            throw BankException.insufficientFunds();
        }

        source.setAccountBalance(source.getAccountBalance().subtract(request.getAmount()));
        destination.setAccountBalance(destination.getAccountBalance().add(request.getAmount()));
        userRepository.save(source);
        userRepository.save(destination);

        return accountResponse(AccountUtils.TRANSFER_SUCCESS_CODE,
                AccountUtils.TRANSFER_SUCCESS_MESSAGE, source);
    }

    private User lockAccount(String accountNumber) {
        return userRepository.findWithLockByAccountNumber(accountNumber)
                .orElseThrow(BankException::accountNotFound);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = AccountUtils.generateAccountNumber();
        } while (userRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    /** Joins the name parts that are present, so a missing other name does not print "null". */
    private String fullName(User user) {
        return Stream.of(user.getFirstName(), user.getLastName(), user.getOtherName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    private BankResponse accountResponse(String code, String message, User user) {
        return BankResponse.builder()
                .responseCode(code)
                .responseMessage(message)
                .accountInfo(AccountInfo.builder()
                        .AccountName(fullName(user))
                        .AccountBalance(user.getAccountBalance())
                        .AccountNumber(user.getAccountNumber())
                        .build())
                .build();
    }
}
