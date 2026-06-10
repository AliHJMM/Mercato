package com.buyapp.product.service;

import com.buyapp.product.dto.ProductDto;
import com.buyapp.product.dto.ProductRequest;
import com.buyapp.product.dto.ProductSearchResponse;
import com.buyapp.product.entity.Product;
import com.buyapp.product.event.ProductEvent;
import com.buyapp.product.exception.AppException;
import com.buyapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final String TOPIC_PRODUCT_EVENTS = "product-events";
    private static final String EVENT_PRODUCT_CREATED = "PRODUCT_CREATED";
    private static final String EVENT_PRODUCT_UPDATED = "PRODUCT_UPDATED";
    private static final String EVENT_PRODUCT_DELETED = "PRODUCT_DELETED";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_PRICE = "price";
    private static final String LOG_PUBLISH_FAILED = "Failed to publish product event: {}";

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MongoTemplate mongoTemplate;

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

    public ProductSearchResponse searchProducts(String q, String category,
                                                Double minPrice, Double maxPrice,
                                                String sort, int page, int size) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        if (StringUtils.hasText(q)) {
            criteria.add(new Criteria().orOperator(
                    Criteria.where("name").regex(q, "i"),
                    Criteria.where("description").regex(q, "i"),
                    Criteria.where(FIELD_CATEGORY).regex(q, "i")
            ));
        }
        if (StringUtils.hasText(category)) {
            criteria.add(Criteria.where(FIELD_CATEGORY).regex(category, "i"));
        }
        if (minPrice != null) {
            criteria.add(Criteria.where(FIELD_PRICE).gte(minPrice));
        }
        if (maxPrice != null) {
            criteria.add(Criteria.where(FIELD_PRICE).lte(maxPrice));
        }
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        Sort sortSpec = switch (sort != null ? sort : "newest") {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, FIELD_PRICE);
            case "price_desc" -> Sort.by(Sort.Direction.DESC, FIELD_PRICE);
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        long total = mongoTemplate.count(query, Product.class);

        query.with(PageRequest.of(page, size, sortSpec));
        List<ProductDto> products = mongoTemplate.find(query, Product.class)
                .stream()
                .map(ProductDto::from)
                .toList();

        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        return ProductSearchResponse.builder()
                .products(products)
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    public List<String> getDistinctCategories() {
        return mongoTemplate.findDistinct(FIELD_CATEGORY, Product.class, String.class)
                .stream()
                .filter(c -> c != null && !c.isBlank())
                .sorted()
                .toList();
    }

    public ProductDto createProduct(ProductRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String sellerId = (String) auth.getPrincipal();
        String sellerName = sellerId;

        if (auth.getDetails() instanceof io.jsonwebtoken.Claims claims) {
            String username = claims.get("username", String.class);
            if (username != null) sellerName = username;
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .sellerId(sellerId)
                .sellerName(sellerName)
                .imageUrls(request.getImageUrls())
                .build();

        product = productRepository.save(product);

        try {
            kafkaTemplate.send(TOPIC_PRODUCT_EVENTS, ProductEvent.builder()
                    .eventType(EVENT_PRODUCT_CREATED)
                    .productId(product.getId())
                    .productName(product.getName())
                    .sellerId(product.getSellerId())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn(LOG_PUBLISH_FAILED, e.getMessage());
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
        product.setCategory(request.getCategory());
        if (request.getImageUrls() != null) {
            product.setImageUrls(request.getImageUrls());
        }

        try {
            kafkaTemplate.send(TOPIC_PRODUCT_EVENTS, ProductEvent.builder()
                    .eventType(EVENT_PRODUCT_UPDATED)
                    .productId(product.getId())
                    .productName(product.getName())
                    .sellerId(product.getSellerId())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn(LOG_PUBLISH_FAILED, e.getMessage());
        }

        return ProductDto.from(productRepository.save(product));
    }

    public void deleteProduct(String id) {
        Product product = findById(id);
        enforceOwnership(product);

        productRepository.deleteById(id);

        try {
            kafkaTemplate.send(TOPIC_PRODUCT_EVENTS, ProductEvent.builder()
                    .eventType(EVENT_PRODUCT_DELETED)
                    .productId(id)
                    .productName(product.getName())
                    .sellerId(product.getSellerId())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn(LOG_PUBLISH_FAILED, e.getMessage());
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
