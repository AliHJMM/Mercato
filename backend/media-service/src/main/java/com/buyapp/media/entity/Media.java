package com.buyapp.media.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

    @Id
    private String id;

    private String filename;         // Object name in MinIO

    private String originalFilename;

    private String mimeType;

    private Long size;

    private String url;              // Publicly accessible URL

    private String uploadedBy;       // sellerId

    private String productId;        // Optional: linked product

    @CreatedDate
    private LocalDateTime createdAt;
}
