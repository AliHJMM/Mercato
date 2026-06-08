package com.buyapp.order.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddress {
    private String fullName;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phone;
}
