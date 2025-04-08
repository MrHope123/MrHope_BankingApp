package com.MrHope123.MrHope_BankingApp.utils;

import java.time.Year;

public class AccountUtils {

    public static final String ACCOUNT_EXISTS_CODE="001";

    public static final String ACCOUNT_EXISTS_MESSAGE="This User already has an account created";

    public static final String ACCOUNT_CREATION_SUCCESS_CODE="002";

    public static final String ACCOUNT_CREATION_SUCCESS_MESSAGE="Account has been successfully created";


    public static String generateAccountNumber(){
        /*
         * 2025 + randomSixDigits
         * */
        Year currentYear =Year.now();
        int min = 1000000;
        int max = 999999;

        //generate random number

        int randNumber = (int)Math.floor(Math.random()* (max - min + 1) + min);

        //Convert current year and random number to String

        String year = String.valueOf(currentYear);
        String randomNumber = String.valueOf(randNumber);

        StringBuilder accountNumber = new StringBuilder();

        return accountNumber.append(year).append(randomNumber).toString();
    }
}
