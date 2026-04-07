package com.buyapp.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(
        regexp = "^[a-zA-Z]+$",
        message = "Username must contain letters only (a-z, A-Z)"
    )
    private String username;

    private String avatarUrl;
}
