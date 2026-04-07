package com.buyapp.media.dto;

import com.buyapp.media.entity.Media;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDto {
    private String id;
    private String originalFilename;
    private String mimeType;
    private Long size;
    private String url;
    private String uploadedBy;
    private String productId;
    private LocalDateTime createdAt;

    public static MediaDto from(Media media) {
        return MediaDto.builder()
                .id(media.getId())
                .originalFilename(media.getOriginalFilename())
                .mimeType(media.getMimeType())
                .size(media.getSize())
                .url(media.getUrl())
                .uploadedBy(media.getUploadedBy())
                .productId(media.getProductId())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
