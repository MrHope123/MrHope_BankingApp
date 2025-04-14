package com.MrHope123.MrHope_BankingApp.utils;

import java.time.Year;

public class AccountUtils {

    public static final String ACCOUNT_EXISTS_CODE="001";

    public static final String ACCOUNT_EXISTS_MESSAGE="This User already has an account created";

    public static final String ACCOUNT_CREATION_SUCCESS_CODE="002";

    public static final String ACCOUNT_CREATION_SUCCESS_MESSAGE="Account has been successfully created";

    public static  final String ACCOUNT_NOT_EXIST_CODE ="003";

    public static final String ACCOUNT_NOT_EXIST_MESSAGE="User with the provided account number doesnt exist";

    public static  final  String ACCOUNT_FOUND_CODE = "004";

    public static final String ACCOUNT_FOUND_MESSAGE= "User account found";

    public static String generateAccountNumber() {
        /*
         * 2025 + randomSixDigits
         * */
        Year currentYear = Year.now();
        int min = 100000; // Changed to 6 digits
        int max = 999999;

        //generate random number

        int randNumber = (int) Math.floor(Math.random() * (max - min + 1) + min);

        //Convert current year and random number to String

        String year = String.valueOf(currentYear);
        String randomNumber = String.valueOf(randNumber);

        StringBuilder accountNumber = new StringBuilder();

        return accountNumber.append(year).append(randomNumber).toString();
    }
}
