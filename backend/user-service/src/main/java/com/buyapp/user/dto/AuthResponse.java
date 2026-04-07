package com.buyapp.user.dto;

import com.buyapp.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String id;
    private String username;
    private String email;
    private Role role;
    private String avatarUrl;
}
