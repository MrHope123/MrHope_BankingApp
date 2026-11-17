package com.MrHope123.MrHope_BankingApp.controller;

import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.CreditDebitRequest;
import com.MrHope123.MrHope_BankingApp.dto.TransferRequest;
import com.MrHope123.MrHope_BankingApp.dto.UserRequest;
import com.MrHope123.MrHope_BankingApp.service.impl.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Create accounts, check balances, credit, debit and transfer")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a new account with a zero balance")
    @PostMapping
    public ResponseEntity<BankResponse> createAccount(@Valid @RequestBody UserRequest userRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createAccount(userRequest));
    }

    @Operation(summary = "Get the account name and balance")
    @GetMapping("/{accountNumber}/balance")
    public BankResponse balanceEnquiry(@PathVariable String accountNumber) {
        return userService.balanceEnquiry(accountNumber);
    }

    @Operation(summary = "Get the account holder's full name")
    @GetMapping("/{accountNumber}/name")
    public String nameEnquiry(@PathVariable String accountNumber) {
        return userService.nameEnquiry(accountNumber);
    }

    @Operation(summary = "Add money to an account")
    @PostMapping("/credit")
    public BankResponse creditAccount(@Valid @RequestBody CreditDebitRequest request) {
        return userService.creditAccount(request);
    }

    @Operation(summary = "Take money from an account (fails if the balance is too low)")
    @PostMapping("/debit")
    public BankResponse debitAccount(@Valid @RequestBody CreditDebitRequest request) {
        return userService.debitAccount(request);
    }

    @Operation(summary = "Move money between two accounts in one atomic step")
    @PostMapping("/transfer")
    public BankResponse transfer(@Valid @RequestBody TransferRequest request) {
        return userService.transfer(request);
    }
}
