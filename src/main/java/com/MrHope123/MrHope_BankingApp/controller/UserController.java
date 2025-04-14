package com.MrHope123.MrHope_BankingApp.controller;

import com.MrHope123.MrHope_BankingApp.dto.BankResponse;
import com.MrHope123.MrHope_BankingApp.dto.EnquiryRequest;
import com.MrHope123.MrHope_BankingApp.dto.UserRequest;
import com.MrHope123.MrHope_BankingApp.service.impl.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping
    public BankResponse createAccount(@RequestBody UserRequest userRequest){
        return userService.createAccount(userRequest);
    }

    @GetMapping("/balanceEnquiry")
    public BankResponse balanceEnquiry(@RequestBody EnquiryRequest enquiryRequest){
        return userService.balanceEnquiry(enquiryRequest);
    }

    @GetMapping("/nameEnquiry")
    public String nameEnquiry(@RequestBody EnquiryRequest enquiryRequest){
        return userService.nameEnquiry(enquiryRequest);
    }
}
