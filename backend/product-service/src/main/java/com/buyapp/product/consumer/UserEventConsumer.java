package com.buyapp.product.consumer;

import com.buyapp.product.entity.Product;
import com.buyapp.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "product-service")
    public void handleUserEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.path("eventType").asText();

            if ("USER_UPDATED".equals(eventType)) {
                String userId = event.path("userId").asText();
                String newUsername = event.path("username").asText();

                if (!userId.isBlank() && !newUsername.isBlank()) {
                    List<Product> products = productRepository.findBySellerId(userId);
                    if (!products.isEmpty()) {
                        products.forEach(p -> p.setSellerName(newUsername));
                        productRepository.saveAll(products);
                        log.info("Updated sellerName to '{}' on {} product(s) for seller {}", newUsername, products.size(), userId);
                    }
                }
            } else if ("USER_DELETED".equals(eventType)) {
                String userId = event.path("userId").asText();
                if (!userId.isBlank()) {
                    List<Product> products = productRepository.findBySellerId(userId);
                    if (!products.isEmpty()) {
                        productRepository.deleteAll(products);
                        log.info("Deleted {} product(s) for deleted seller {}", products.size(), userId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process user event: {}", e.getMessage());
        }
    }
}
