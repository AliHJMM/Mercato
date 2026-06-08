package com.buyapp.product.dto;

import com.buyapp.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private String sellerId;
    private String sellerName;
    private String category;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductDto from(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .sellerId(product.getSellerId())
                .sellerName(product.getSellerName())
                .category(product.getCategory())
                .imageUrls(product.getImageUrls())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
