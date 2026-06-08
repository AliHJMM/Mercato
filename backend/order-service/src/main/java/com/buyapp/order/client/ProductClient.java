package com.buyapp.order.client;

import com.buyapp.order.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${product-service.url}")
    private String productServiceUrl;

    public ProductInfo getProduct(String productId) {
        try {
            return restTemplate.getForObject(
                    productServiceUrl + "/products/internal/" + productId,
                    ProductInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new AppException("Product not found: " + productId, HttpStatus.NOT_FOUND);
        } catch (HttpClientErrorException e) {
            throw new AppException("Product service error: " + e.getMessage(),
                    HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error calling product-service for product {}: {}", productId, e.getMessage());
            throw new AppException("Product service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public void reduceStock(List<StockRequest> items) {
        try {
            restTemplate.postForObject(
                    productServiceUrl + "/products/internal/reduce-stock",
                    items, Void.class);
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            throw new AppException(
                    body.contains("Insufficient") ? extractMessage(body) : "Stock reduction failed",
                    HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error reducing stock: {}", e.getMessage());
            throw new AppException("Product service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public void restoreStock(List<StockRequest> items) {
        try {
            restTemplate.postForObject(
                    productServiceUrl + "/products/internal/restore-stock",
                    items, Void.class);
        } catch (Exception e) {
            log.warn("Failed to restore stock (non-critical): {}", e.getMessage());
        }
    }

    private String extractMessage(String responseBody) {
        int start = responseBody.indexOf("\"message\":\"");
        if (start == -1) return "Stock operation failed";
        start += 11;
        int end = responseBody.indexOf("\"", start);
        return end == -1 ? "Stock operation failed" : responseBody.substring(start, end);
    }
}
