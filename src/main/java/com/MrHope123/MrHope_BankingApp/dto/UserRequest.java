package com.MrHope123.MrHope_BankingApp.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String otherName;
    private String gender;
    private String address;
    private String stateOfOrigin;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Phone number must be 9 to 15 digits")
    private String phoneNumber;

    @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "Alternative phone number must be 9 to 15 digits")
    private String alternativePhoneNumber;

}
