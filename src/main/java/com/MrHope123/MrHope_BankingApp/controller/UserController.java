package com.MrHope123.MrHope_BankingApp.controller;

import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.CreditDebitRequest;
import com.MrHope123.MrHope_BankingApp.dto.TransferRequest;
import com.MrHope123.MrHope_BankingApp.service.impl.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Every method needs a valid JWT. The token's subject (the customer's email) is handed to the
 * service layer, which refuses any account that does not belong to that customer.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Accounts", description = "Balance, credit, debit and transfer on your own account")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get your account name and balance")
    @GetMapping("/{accountNumber}/balance")
    public BankResponse balanceEnquiry(@PathVariable String accountNumber, Principal principal) {
        return userService.balanceEnquiry(principal.getName(), accountNumber);
    }

    @Operation(summary = "Get the account holder's full name")
    @GetMapping("/{accountNumber}/name")
    public String nameEnquiry(@PathVariable String accountNumber, Principal principal) {
        return userService.nameEnquiry(principal.getName(), accountNumber);
    }

    @Operation(summary = "Add money to your account")
    @PostMapping("/credit")
    public BankResponse creditAccount(@Valid @RequestBody CreditDebitRequest request, Principal principal) {
        return userService.creditAccount(principal.getName(), request);
    }

    @Operation(summary = "Take money from your account (fails if the balance is too low)")
    @PostMapping("/debit")
    public BankResponse debitAccount(@Valid @RequestBody CreditDebitRequest request, Principal principal) {
        return userService.debitAccount(principal.getName(), request);
    }

    @Operation(summary = "Move money from your account to any other account in one atomic step")
    @PostMapping("/transfer")
    public BankResponse transfer(@Valid @RequestBody TransferRequest request, Principal principal) {
        return userService.transfer(principal.getName(), request);
    }
}
