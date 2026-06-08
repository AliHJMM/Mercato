package com.buyapp.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductSearchResponse {
    private List<ProductDto> products;
    private long total;
    private int page;
    private int size;
    private int totalPages;
}
