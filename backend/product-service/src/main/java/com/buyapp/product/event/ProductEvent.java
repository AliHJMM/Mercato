package com.buyapp.product.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    private String eventType;
    private String productId;
    private String productName;
    private String sellerId;
    private LocalDateTime timestamp;
}
