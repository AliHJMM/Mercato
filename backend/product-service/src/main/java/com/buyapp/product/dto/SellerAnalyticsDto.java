package com.buyapp.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SellerAnalyticsDto {
    private Double totalRevenue;
    private Integer totalOrdersProcessed;
    private Integer totalUnitsSold;
    private List<ProductSalesDto> bestSellingProducts;

    @Data
    @Builder
    public static class ProductSalesDto {
        private String productId;
        private String productName;
        private Integer unitsSold;
        private Double revenue;
    }
}
