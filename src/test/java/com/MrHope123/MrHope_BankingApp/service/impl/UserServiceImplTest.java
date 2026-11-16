package com.MrHope123.MrHope_BankingApp.service.impl;

import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.CreditDebitRequest;
import com.MrHope123.MrHope_BankingApp.dto.TransferRequest;
import com.MrHope123.MrHope_BankingApp.dto.UserRequest;
import com.MrHope123.MrHope_BankingApp.entity.User;
import com.MrHope123.MrHope_BankingApp.exception.BankException;
import com.MrHope123.MrHope_BankingApp.repository.UserRepository;
import com.MrHope123.MrHope_BankingApp.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String ACC_A = "2026000001";
    private static final String ACC_B = "2026000002";

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
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getAccountInfo().getAccountBalance()));
        assertEquals("Thabo Mokoena J", response.getAccountInfo().getAccountName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAccount_withoutOtherName_doesNotPrintNull() {
        UserRequest request = sampleRequest();
        request.setOtherName(null);
        when(userRepository.existsByEmail("thabo@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        BankResponse response = userService.createAccount(request);

        assertEquals("Thabo Mokoena", response.getAccountInfo().getAccountName());
    }

    @Test
    void createAccount_normalisesEmailBeforeCheckingForDuplicates() {
        UserRequest request = sampleRequest();
        request.setEmail("  Thabo@Example.COM ");
        when(userRepository.existsByEmail("thabo@example.com")).thenReturn(true);

        BankException ex = assertThrows(BankException.class, () -> userService.createAccount(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createAccount_existingEmail_throwsAccountExistsAndDoesNotSave() {
        when(userRepository.existsByEmail("thabo@example.com")).thenReturn(true);

        BankException ex = assertThrows(BankException.class, () -> userService.createAccount(sampleRequest()));

        assertEquals(AccountUtils.ACCOUNT_EXISTS_CODE, ex.getResponseCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    // ---- balanceEnquiry / nameEnquiry ----

    @Test
    void balanceEnquiry_existingAccount_returnsBalance() {
        when(userRepository.findByAccountNumber(ACC_A)).thenReturn(Optional.of(userWithBalance(ACC_A, "250.00")));

        BankResponse response = userService.balanceEnquiry(ACC_A);

        assertEquals(AccountUtils.ACCOUNT_FOUND_CODE, response.getResponseCode());
        assertEquals(0, new BigDecimal("250.00").compareTo(response.getAccountInfo().getAccountBalance()));
        assertEquals(ACC_A, response.getAccountInfo().getAccountNumber());
    }

    @Test
    void balanceEnquiry_unknownAccount_throwsNotFound() {
        when(userRepository.findByAccountNumber("0000000000")).thenReturn(Optional.empty());

        BankException ex = assertThrows(BankException.class, () -> userService.balanceEnquiry("0000000000"));

        assertEquals(AccountUtils.ACCOUNT_NOT_EXIST_CODE, ex.getResponseCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void nameEnquiry_existingAccount_returnsFullName() {
        when(userRepository.findByAccountNumber(ACC_A)).thenReturn(Optional.of(userWithBalance(ACC_A, "0")));

        assertEquals("Thabo Mokoena J", userService.nameEnquiry(ACC_A));
    }

    @Test
    void nameEnquiry_unknownAccount_throwsNotFound() {
        when(userRepository.findByAccountNumber("0000000000")).thenReturn(Optional.empty());

        assertThrows(BankException.class, () -> userService.nameEnquiry("0000000000"));
    }

    // ---- credit ----

    @Test
    void credit_existingAccount_increasesBalance() {
        User user = userWithBalance(ACC_A, "100.00");
        when(userRepository.findWithLockByAccountNumber(ACC_A)).thenReturn(Optional.of(user));

        BankResponse response = userService.creditAccount(new CreditDebitRequest(ACC_A, new BigDecimal("50.00")));

        assertEquals(AccountUtils.ACCOUNT_CREDIT_SUCCESS_CODE, response.getResponseCode());
        assertEquals(0, new BigDecimal("150.00").compareTo(response.getAccountInfo().getAccountBalance()));
        verify(userRepository).save(user);
    }

    @Test
    void credit_unknownAccount_throwsNotFoundAndDoesNotSave() {
        when(userRepository.findWithLockByAccountNumber("0000000000")).thenReturn(Optional.empty());

        assertThrows(BankException.class,
                () -> userService.creditAccount(new CreditDebitRequest("0000000000", new BigDecimal("50.00"))));
        verify(userRepository, never()).save(any(User.class));
    }

    // ---- debit ----

    @Test
    void debit_enoughFunds_decreasesBalance() {
        User user = userWithBalance(ACC_A, "100.00");
        when(userRepository.findWithLockByAccountNumber(ACC_A)).thenReturn(Optional.of(user));

        BankResponse response = userService.debitAccount(new CreditDebitRequest(ACC_A, new BigDecimal("40.00")));

        assertEquals(AccountUtils.ACCOUNT_DEBIT_SUCCESS_CODE, response.getResponseCode());
        assertEquals(0, new BigDecimal("60.00").compareTo(response.getAccountInfo().getAccountBalance()));
        verify(userRepository).save(user);
    }

    @Test
    void debit_exactBalance_isAllowedAndLeavesZero() {
        User user = userWithBalance(ACC_A, "100.00");
        when(userRepository.findWithLockByAccountNumber(ACC_A)).thenReturn(Optional.of(user));

        BankResponse response = userService.debitAccount(new CreditDebitRequest(ACC_A, new BigDecimal("100.00")));

        assertEquals(0, BigDecimal.ZERO.compareTo(response.getAccountInfo().getAccountBalance()));
    }

    @Test
    void debit_insufficientFunds_throwsAndLeavesBalanceUntouched() {
        User user = userWithBalance(ACC_A, "30.00");
        when(userRepository.findWithLockByAccountNumber(ACC_A)).thenReturn(Optional.of(user));

        BankException ex = assertThrows(BankException.class,
                () -> userService.debitAccount(new CreditDebitRequest(ACC_A, new BigDecimal("30.01"))));

        assertEquals(AccountUtils.INSUFFICIENT_FUNDS_CODE, ex.getResponseCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals(0, new BigDecimal("30.00").compareTo(user.getAccountBalance()));
        verify(userRepository, never()).save(any(User.class));
    }

    // ---- transfer ----

    @Test
    void transfer_movesMoneyFromSourceToDestination() {
        User source = userWithBalance(ACC_A, "100.00");
        User destination = userWithBalance(ACC_B, "10.00");
        when(userRepository.findWithLockByAccountNumber(ACC_A)).thenReturn(Optional.of(source));
        when(userRepository.findWithLockByAccountNumber(ACC_B)).thenReturn(Optional.of(destination));

        BankResponse response = userService.transfer(new TransferRequest(ACC_A, ACC_B, new BigDecimal("25.00")));

        assertEquals(AccountUtils.TRANSFER_SUCCESS_CODE, response.getResponseCode());
        assertEquals(0, new BigDecimal("75.00").compareTo(source.getAccountBalance()));
        assertEquals(0, new BigDecimal("35.00").compareTo(destination.getAccountBalance()));
        verify(userRepository).save(source);
        verify(userRepository).save(destination);
    }

    @Test
    void transfer_worksWhenSourceNumberIsHigherThanDestination() {
        User source = userWithBalance(ACC_B, "100.00");
        User destination = userWithBalance(ACC_A, "0.00");
        when(userRepository.findWithLockByAccountNumber(ACC_A)).thenReturn(Optional.of(destination));
        when(userRepository.findWithLockByAccountNumber(ACC_B)).thenReturn(Optional.of(source));

        userService.transfer(new TransferRequest(ACC_B, ACC_A, new BigDecimal("40.00")));

        assertEquals(0, new BigDecimal("60.00").compareTo(source.getAccountBalance()));
        assertEquals(0, new BigDecimal("40.00").compareTo(destination.getAccountBalance()));
    }

    @Test
    void transfer_insufficientFunds_changesNothing() {
        User source = userWithBalance(ACC_A, "10.00");
        User destination = userWithBalance(ACC_B, "0.00");
        when(userRepository.findWithLockByAccountNumber(ACC_A)).thenReturn(Optional.of(source));
        when(userRepository.findWithLockByAccountNumber(ACC_B)).thenReturn(Optional.of(destination));

        BankException ex = assertThrows(BankException.class,
                () -> userService.transfer(new TransferRequest(ACC_A, ACC_B, new BigDecimal("10.01"))));

        assertEquals(AccountUtils.INSUFFICIENT_FUNDS_CODE, ex.getResponseCode());
        assertEquals(0, new BigDecimal("10.00").compareTo(source.getAccountBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(destination.getAccountBalance()));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void transfer_toSameAccount_isRejectedBeforeAnyLookup() {
        BankException ex = assertThrows(BankException.class,
                () -> userService.transfer(new TransferRequest(ACC_A, ACC_A, new BigDecimal("5.00"))));

        assertEquals(AccountUtils.SAME_ACCOUNT_TRANSFER_CODE, ex.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(userRepository);
    }

    @Test
    void transfer_unknownDestination_throwsNotFound() {
        when(userRepository.findWithLockByAccountNumber(ACC_A)).thenReturn(Optional.of(userWithBalance(ACC_A, "50.00")));
        when(userRepository.findWithLockByAccountNumber(ACC_B)).thenReturn(Optional.empty());

        BankException ex = assertThrows(BankException.class,
                () -> userService.transfer(new TransferRequest(ACC_A, ACC_B, new BigDecimal("5.00"))));

        assertEquals(AccountUtils.ACCOUNT_NOT_EXIST_CODE, ex.getResponseCode());
        verify(userRepository, never()).save(any(User.class));
    }
}
