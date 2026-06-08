package com.buyapp.order.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserAnalyticsDto {
    private Double totalSpent;
    private Integer totalOrders;
    private List<ProductPurchaseDto> mostBoughtProducts;
    private List<CategorySpendDto> topCategories;

    @Data
    @Builder
    public static class ProductPurchaseDto {
        private String productId;
        private String productName;
        private Integer totalQuantity;
        private Double totalSpent;
    }

    @Data
    @Builder
    public static class CategorySpendDto {
        private String category;
        private Double totalSpent;
        private Integer totalItems;
    }
}
