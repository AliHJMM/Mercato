package com.buyapp.product.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    private String productId;
    private String productName;
    private String sellerName;
    private String imageUrl;
    private Double price;
    private Integer quantity;
    private Double subtotal;
}
