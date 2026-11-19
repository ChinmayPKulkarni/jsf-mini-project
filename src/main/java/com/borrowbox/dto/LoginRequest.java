package com.borrowbox.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email or USN is required")
    private String username; // can be email or USN

    @NotBlank(message = "Password is required")
    private String password;
}

