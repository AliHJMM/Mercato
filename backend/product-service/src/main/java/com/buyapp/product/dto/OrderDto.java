package com.buyapp.product.dto;

import com.buyapp.product.entity.Order;
import com.buyapp.product.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDto {

    private String id;
    private String buyerId;
    private List<OrderItemDto> items;
    private Double total;
    private String status;
    private String paymentMethod;
    private DeliveryAddressDto deliveryAddress;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class OrderItemDto {
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

    @Data
    @Builder
    public static class DeliveryAddressDto {
        private String fullName;
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private String phone;
    }

    public static OrderDto from(Order order) {
        DeliveryAddressDto addressDto = null;
        if (order.getDeliveryAddress() != null) {
            addressDto = DeliveryAddressDto.builder()
                    .fullName(order.getDeliveryAddress().getFullName())
                    .street(order.getDeliveryAddress().getStreet())
                    .city(order.getDeliveryAddress().getCity())
                    .state(order.getDeliveryAddress().getState())
                    .zipCode(order.getDeliveryAddress().getZipCode())
                    .country(order.getDeliveryAddress().getCountry())
                    .phone(order.getDeliveryAddress().getPhone())
                    .build();
        }

        return OrderDto.builder()
                .id(order.getId())
                .buyerId(order.getBuyerId())
                .items(order.getItems() == null ? List.of() : order.getItems().stream()
                        .map(item -> OrderItemDto.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .sellerId(item.getSellerId())
                                .sellerName(item.getSellerName())
                                .imageUrl(item.getImageUrl())
                                .category(item.getCategory())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .subtotal(item.getSubtotal())
                                .build())
                        .toList())
                .total(order.getTotal())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .deliveryAddress(addressDto)
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
