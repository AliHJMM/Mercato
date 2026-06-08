package com.buyapp.order.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    private String id;
    private String buyerId;
    private List<OrderItem> items;
    private Double total;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private DeliveryAddress deliveryAddress;
    private LocalDateTime cancelledAt;
    @CreatedDate
    private LocalDateTime createdAt;
}
