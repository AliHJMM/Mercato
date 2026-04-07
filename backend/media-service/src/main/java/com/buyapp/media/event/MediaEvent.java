package com.buyapp.media.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaEvent {
    private String eventType;
    private String mediaId;
    private String filename;
    private String url;
    private String uploadedBy;
    private String productId;
    private LocalDateTime timestamp;
}
