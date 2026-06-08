package com.buyapp.order.client;

import lombok.Data;

import java.util.List;

@Data
public class ProductInfo {
    private String id;
    private String name;
    private Double price;
    private Integer quantity;
    private String sellerId;
    private String sellerName;
    private List<String> imageUrls;
    private String category;
}
