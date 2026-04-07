package com.buyapp.product.dto;

import com.buyapp.product.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class OrderDto {

    private String id;
    private String buyerId;
    private List<OrderItemDto> items;
    private Double total;
    private String status;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class OrderItemDto {
        private String productId;
        private String productName;
        private String sellerName;
        private String imageUrl;
        private Double price;
        private Integer quantity;
        private Double subtotal;
    }

    public static OrderDto from(Order order) {
        return OrderDto.builder()
                .id(order.getId())
                .buyerId(order.getBuyerId())
                .items(order.getItems() == null ? List.of() : order.getItems().stream()
                        .map(item -> OrderItemDto.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .sellerName(item.getSellerName())
                                .imageUrl(item.getImageUrl())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .subtotal(item.getSubtotal())
                                .build())
                        .collect(Collectors.toList()))
                .total(order.getTotal())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
