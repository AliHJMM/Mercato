package com.buyapp.user.controller;

import com.buyapp.user.dto.UserDto;
import com.buyapp.user.dto.UserUpdateRequest;
import com.buyapp.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMe(Authentication authentication,
                                            @Valid @RequestBody UserUpdateRequest request) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(userService.updateMe(userId, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        userService.deleteMe(userId);
        return ResponseEntity.noContent().build();
    }
}
