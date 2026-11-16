package com.MrHope123.MrHope_BankingApp.utils;

import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

class AccountUtilsTest {

    @Test
    void generateAccountNumber_startsWithCurrentYearAndHasTenDigits() {
        String accountNumber = AccountUtils.generateAccountNumber();

        assertEquals(10, accountNumber.length());
        assertTrue(accountNumber.startsWith(String.valueOf(Year.now())));
        assertTrue(accountNumber.chars().allMatch(Character::isDigit));
    }
}
