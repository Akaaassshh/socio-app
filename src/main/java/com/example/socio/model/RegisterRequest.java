//package com.example.socio.model;
//
//
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import lombok.Getter;
//import lombok.Setter;
//
//@Getter
//@Setter
//public class RegisterRequest {
//
//    @NotBlank
//    @Email
//    private String email;
//
//    @NotBlank
//    private String password;
//
//    @NotBlank
//    private String role;
//
//    // Getters and Setters
//}

package com.example.socio.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private LocalDate dob;

    private boolean isPrivate;

    // Role will default to USER in the service layer
    private String role = "USER";

    // Getters and Setters
}