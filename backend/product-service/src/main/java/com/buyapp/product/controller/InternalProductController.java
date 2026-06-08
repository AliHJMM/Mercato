package com.buyapp.product.controller;

import com.buyapp.product.dto.ProductInfo;
import com.buyapp.product.dto.StockRequest;
import com.buyapp.product.entity.Product;
import com.buyapp.product.exception.AppException;
import com.buyapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/products/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalProductController {

    private final ProductRepository productRepository;

    @GetMapping("/{id}")
    public ResponseEntity<ProductInfo> getProductInfo(@PathVariable String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException("Product not found: " + id, HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(ProductInfo.from(product));
    }

    @PostMapping("/reduce-stock")
    public ResponseEntity<Void> reduceStock(@RequestBody List<StockRequest> items) {
        List<Product> validated = new ArrayList<>();

        for (StockRequest item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new AppException(
                            "Product not found: " + item.getProductId(), HttpStatus.NOT_FOUND));
            if (product.getQuantity() < item.getQuantity()) {
                throw new AppException(
                        "Insufficient stock for \"" + product.getName() + "\". Available: " + product.getQuantity(),
                        HttpStatus.CONFLICT);
            }
            validated.add(product);
        }

        for (int i = 0; i < validated.size(); i++) {
            Product product = validated.get(i);
            product.setQuantity(product.getQuantity() - items.get(i).getQuantity());
            productRepository.save(product);
        }

        log.debug("Reduced stock for {} products", items.size());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restore-stock")
    public ResponseEntity<Void> restoreStock(@RequestBody List<StockRequest> items) {
        for (StockRequest item : items) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setQuantity(product.getQuantity() + item.getQuantity());
                productRepository.save(product);
            });
        }
        log.debug("Restored stock for {} products", items.size());
        return ResponseEntity.ok().build();
    }
}
