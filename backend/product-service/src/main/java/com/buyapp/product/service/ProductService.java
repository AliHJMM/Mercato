package com.buyapp.product.service;

import com.buyapp.product.dto.ProductDto;
import com.buyapp.product.dto.ProductRequest;
import com.buyapp.product.entity.Product;
import com.buyapp.product.event.ProductEvent;
import com.buyapp.product.exception.AppException;
import com.buyapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductDto::from)
                .toList();
    }

    public ProductDto getProductById(String id) {
        return ProductDto.from(findById(id));
    }

    public List<ProductDto> getMyProducts() {
        String sellerId = getCurrentUserId();
        return productRepository.findBySellerId(sellerId)
                .stream()
                .map(ProductDto::from)
                .toList();
    }

    public ProductDto createProduct(ProductRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String sellerId = (String) auth.getPrincipal();
        String sellerName = sellerId; // Could be fetched from user-service, using id for now

        // Extract username from JWT claims if available
        if (auth.getDetails() instanceof io.jsonwebtoken.Claims claims) {
            String username = claims.get("username", String.class);
            if (username != null) sellerName = username;
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .sellerId(sellerId)
                .sellerName(sellerName)
                .imageUrls(request.getImageUrls())
                .build();

        product = productRepository.save(product);

        // Publish PRODUCT_CREATED event
        try {
            kafkaTemplate.send("product-events", ProductEvent.builder()
                    .eventType("PRODUCT_CREATED")
                    .productId(product.getId())
                    .productName(product.getName())
                    .sellerId(product.getSellerId())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish product event: {}", e.getMessage());
        }

        return ProductDto.from(product);
    }

    public ProductDto updateProduct(String id, ProductRequest request) {
        Product product = findById(id);
        enforceOwnership(product);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        if (request.getImageUrls() != null) {
            product.setImageUrls(request.getImageUrls());
        }

        // Publish PRODUCT_UPDATED event
        try {
            kafkaTemplate.send("product-events", ProductEvent.builder()
                    .eventType("PRODUCT_UPDATED")
                    .productId(product.getId())
                    .productName(product.getName())
                    .sellerId(product.getSellerId())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish product event: {}", e.getMessage());
        }

        return ProductDto.from(productRepository.save(product));
    }

    public void deleteProduct(String id) {
        Product product = findById(id);
        enforceOwnership(product);

        productRepository.deleteById(id);

        try {
            kafkaTemplate.send("product-events", ProductEvent.builder()
                    .eventType("PRODUCT_DELETED")
                    .productId(id)
                    .productName(product.getName())
                    .sellerId(product.getSellerId())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish product event: {}", e.getMessage());
        }
    }

    private Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
    }

    private void enforceOwnership(Product product) {
        String currentUserId = getCurrentUserId();
        if (!product.getSellerId().equals(currentUserId)) {
            throw new AppException("You do not own this product", HttpStatus.FORBIDDEN);
        }
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
