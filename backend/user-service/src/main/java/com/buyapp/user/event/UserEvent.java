package com.buyapp.user.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String eventType;
    private String userId;
    private String username;
    private String email;
    private String role;
    private LocalDateTime timestamp;
}
