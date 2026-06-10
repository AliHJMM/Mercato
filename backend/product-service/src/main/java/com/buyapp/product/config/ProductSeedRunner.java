package com.buyapp.product.config;

import com.buyapp.product.entity.Product;
import com.buyapp.product.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Slf4j
public class ProductSeedRunner implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.seed.force:false}")
    private boolean force;

    @Value("${app.seed.products-resource:seed/products.json}")
    private String productsResource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!force && productRepository.count() > 0) {
            log.info("Skipping product seed because the products collection is not empty.");
            return;
        }

        List<SeedProduct> seedProducts = readProducts();
        if (seedProducts.isEmpty()) {
            log.warn("No product seed data was loaded from {}.", productsResource);
            return;
        }

        if (force) {
            productRepository.deleteAll();
        }

        List<Product> products = seedProducts.stream()
                .map(this::toProduct)
                .toList();


        productRepository.saveAll(products);
        log.info("Seeded {} products from {}.", products.size(), productsResource);
    }

    private List<SeedProduct> readProducts() throws IOException {
        ClassPathResource resource = new ClassPathResource(productsResource);
        if (!resource.exists()) {
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<SeedProduct>>() {
            });
        }
    }

    private Product toProduct(SeedProduct seedProduct) {
        return Product.builder()
                .id(seedProduct.id())
                .name(seedProduct.name())
                .description(seedProduct.description())
                .price(seedProduct.price())
                .quantity(seedProduct.quantity())
                .category(seedProduct.category())
                .sellerId(seedProduct.sellerId())
                .sellerName(seedProduct.sellerName())
                .imageUrls(seedProduct.imageUrls())
                .build();
    }

    private record SeedProduct(
            String id,
            String name,
            String description,
            Double price,
            Integer quantity,
            String category,
            String sellerId,
            String sellerName,
            List<String> imageUrls
    ) {
    }
}
