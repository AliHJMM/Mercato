package com.buyapp.product.dto;

import com.buyapp.product.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductInfo {
    private String id;
    private String name;
    private Double price;
    private Integer quantity;
    private String sellerId;
    private String sellerName;
    private List<String> imageUrls;
    private String category;

    public static ProductInfo from(Product p) {
        return ProductInfo.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .quantity(p.getQuantity())
                .sellerId(p.getSellerId())
                .sellerName(p.getSellerName())
                .imageUrls(p.getImageUrls())
                .category(p.getCategory())
                .build();
    }
}
