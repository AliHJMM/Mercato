package com.buyapp.product.dto;

import lombok.Data;

@Data
public class StockRequest {
    private String productId;
    private Integer quantity;
}
