package com.buyapp.product.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    private String productId;
    private String productName;
    private String sellerId;
    private String sellerName;
    private String imageUrl;
    private String category;
    private Double price;
    private Integer quantity;
    private Double subtotal;
}
