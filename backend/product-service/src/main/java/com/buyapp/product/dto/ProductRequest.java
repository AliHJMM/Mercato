package com.buyapp.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Name must be under 255 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity cannot be negative")
    private Integer quantity;

    private List<String> imageUrls = new ArrayList<>();
}
