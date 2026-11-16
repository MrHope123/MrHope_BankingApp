package com.MrHope123.MrHope_BankingApp.service.impl;

import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.CreditDebitRequest;
import com.MrHope123.MrHope_BankingApp.dto.EnquiryRequest;
import com.MrHope123.MrHope_BankingApp.dto.UserRequest;
import com.MrHope123.MrHope_BankingApp.entity.User;
import com.MrHope123.MrHope_BankingApp.repository.UserRepository;
import com.MrHope123.MrHope_BankingApp.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequest sampleRequest() {
        return UserRequest.builder()
                .firstName("Thabo")
                .lastName("Mokoena")
                .otherName("J")
                .gender("Male")
                .address("1 Main Road")
                .stateOfOrigin("Gauteng")
                .email("thabo@example.com")
                .phoneNumber("0710000000")
                .alternativePhoneNumber("0720000000")
                .build();
    }

    private User userWithBalance(String accountNumber, String balance) {
        return User.builder()
                .firstName("Thabo")
                .lastName("Mokoena")
                .otherName("J")
                .accountNumber(accountNumber)
                .accountBalance(new BigDecimal(balance))
                .build();
    }

    // ---- createAccount ----

    @Test
    void createAccount_newUser_savesAccountWithZeroBalance() {
        when(userRepository.existsByEmail("thabo@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        BankResponse response = userService.createAccount(sampleRequest());

        assertEquals(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE, response.getResponseCode());
        assertEquals(AccountUtils.ACCOUNT_CREATION_SUCCESS_MESSAGE, response.getResponseMessage());
        assertNotNull(response.getAccountInfo());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getAccountInfo().getAccountBalance()));
        assertEquals("Thabo Mokoena J", response.getAccountInfo().getAccountName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAccount_existingEmail_returnsAccountExistsAndDoesNotSave() {
        when(userRepository.existsByEmail("thabo@example.com")).thenReturn(true);

        BankResponse response = userService.createAccount(sampleRequest());

        assertEquals(AccountUtils.ACCOUNT_EXISTS_CODE, response.getResponseCode());
        assertEquals(AccountUtils.ACCOUNT_EXISTS_MESSAGE, response.getResponseMessage());
        assertNull(response.getAccountInfo());
        verify(userRepository, never()).save(any(User.class));
    }

    // ---- balanceEnquiry ----

    @Test
    void balanceEnquiry_existingAccount_returnsBalance() {
        when(userRepository.existsByAccountNumber("2026123456")).thenReturn(true);
        when(userRepository.findByAccountNumber("2026123456"))
                .thenReturn(userWithBalance("2026123456", "250.00"));

        BankResponse response = userService.balanceEnquiry(new EnquiryRequest("2026123456"));

        assertEquals(AccountUtils.ACCOUNT_FOUND_CODE, response.getResponseCode());
        assertEquals(0, new BigDecimal("250.00").compareTo(response.getAccountInfo().getAccountBalance()));
        assertEquals("2026123456", response.getAccountInfo().getAccountNumber());
    }

    @Test
    void balanceEnquiry_unknownAccount_returnsNotFound() {
        when(userRepository.existsByAccountNumber("0000")).thenReturn(false);

        BankResponse response = userService.balanceEnquiry(new EnquiryRequest("0000"));

        assertEquals(AccountUtils.ACCOUNT_NOT_EXIST_CODE, response.getResponseCode());
        assertEquals(AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE, response.getResponseMessage());
        assertNull(response.getAccountInfo());
    }

    // ---- nameEnquiry ----

    @Test
    void nameEnquiry_existingAccount_returnsFullName() {
        when(userRepository.existsByAccountNumber("2026123456")).thenReturn(true);
        when(userRepository.findByAccountNumber("2026123456"))
                .thenReturn(userWithBalance("2026123456", "0"));

        assertEquals("Thabo Mokoena J", userService.nameEnquiry(new EnquiryRequest("2026123456")));
    }

    @Test
    void nameEnquiry_unknownAccount_returnsNotExistMessage() {
        when(userRepository.existsByAccountNumber("0000")).thenReturn(false);

        assertEquals(AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE,
                userService.nameEnquiry(new EnquiryRequest("0000")));
    }

    // ---- creditDebitAccount (credit) ----

    @Test
    void credit_existingAccount_increasesBalance() {
        User user = userWithBalance("2026123456", "100.00");
        when(userRepository.existsByAccountNumber("2026123456")).thenReturn(true);
        when(userRepository.findByAccountNumber("2026123456")).thenReturn(user);

        BankResponse response = userService.creditDebitAccount(
                new CreditDebitRequest("2026123456", new BigDecimal("50.00")));

        assertEquals(AccountUtils.ACCOUNT_CREDIT_SUCCESS_CODE, response.getResponseCode());
        assertEquals(0, new BigDecimal("150.00").compareTo(response.getAccountInfo().getAccountBalance()));
        verify(userRepository).save(user);
    }

    @Test
    void credit_unknownAccount_returnsNotFoundAndDoesNotSave() {
        when(userRepository.existsByAccountNumber("0000")).thenReturn(false);

        BankResponse response = userService.creditDebitAccount(
                new CreditDebitRequest("0000", new BigDecimal("50.00")));

        assertEquals(AccountUtils.ACCOUNT_NOT_EXIST_CODE, response.getResponseCode());
        assertEquals(AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE, response.getResponseMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}
