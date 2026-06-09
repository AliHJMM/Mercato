package com.buyapp.product.service;

import com.buyapp.product.dto.ProductDto;
import com.buyapp.product.dto.ProductRequest;
import com.buyapp.product.dto.ProductSearchResponse;
import com.buyapp.product.entity.Product;
import com.buyapp.product.exception.AppException;
import com.buyapp.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProductService productService;

    private static final String SELLER_ID = "seller-001";

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken(
                SELLER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getProductById_found() {
        Product product = Product.builder()
                .id("prod-1").name("Keyboard").price(29.9).quantity(10)
                .sellerId(SELLER_ID).sellerName("Seller").category("Peripherals")
                .imageUrls(List.of()).build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        ProductDto dto = productService.getProductById("prod-1");

        assertThat(dto.getName()).isEqualTo("Keyboard");
        assertThat(dto.getCategory()).isEqualTo("Peripherals");
    }

    @Test
    void getProductById_notFound_throws() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById("missing"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void createProduct_success() {
        ProductRequest request = new ProductRequest();
        request.setName("Mouse");
        request.setPrice(15.0);
        request.setQuantity(20);
        request.setCategory("Peripherals");
        request.setImageUrls(List.of());

        Product saved = Product.builder()
                .id("prod-2").name("Mouse").price(15.0).quantity(20)
                .category("Peripherals").sellerId(SELLER_ID).sellerName(SELLER_ID)
                .imageUrls(List.of()).build();

        when(productRepository.save(any())).thenReturn(saved);

        ProductDto dto = productService.createProduct(request);

        assertThat(dto.getName()).isEqualTo("Mouse");
        assertThat(dto.getSellerId()).isEqualTo(SELLER_ID);
    }

    @Test
    void deleteProduct_notOwner_throws() {
        Product product = Product.builder()
                .id("prod-3").name("Widget").price(5.0).quantity(5)
                .sellerId("other-seller").sellerName("Other").imageUrls(List.of()).build();

        when(productRepository.findById("prod-3")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.deleteProduct("prod-3"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("do not own");
    }

    @Test
    void searchProducts_returnsPagedResults() {
        Product product = Product.builder()
                .id("prod-1").name("Keyboard").price(29.9).quantity(10)
                .sellerId(SELLER_ID).sellerName("Seller").category("Peripherals")
                .imageUrls(List.of()).build();

        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

        ProductSearchResponse response = productService.searchProducts("keyboard", null, null, null, "newest", 0, 12);

        assertThat(response.getTotal()).isEqualTo(1L);
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    void updateProduct_success() {
        Product product = Product.builder()
                .id("prod-1").name("Old Name").price(10.0).quantity(5)
                .sellerId(SELLER_ID).sellerName("Seller").imageUrls(List.of()).build();

        ProductRequest request = new ProductRequest();
        request.setName("New Name");
        request.setPrice(12.0);
        request.setQuantity(8);
        request.setCategory("Electronics");
        request.setImageUrls(List.of());

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductDto dto = productService.updateProduct("prod-1", request);

        assertThat(dto.getName()).isEqualTo("New Name");
        assertThat(dto.getCategory()).isEqualTo("Electronics");
    }

    @Test
    void getAllProducts_returnsList() {
        Product p1 = Product.builder().id("p1").name("Laptop").price(999.0).quantity(5)
                .sellerId(SELLER_ID).sellerName("Seller").category("Electronics").imageUrls(List.of()).build();
        Product p2 = Product.builder().id("p2").name("Phone").price(499.0).quantity(10)
                .sellerId(SELLER_ID).sellerName("Seller").category("Electronics").imageUrls(List.of()).build();

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductDto> result = productService.getAllProducts();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductDto::getName).containsExactly("Laptop", "Phone");
    }

    @Test
    void getMyProducts_returnsSellersProducts() {
        Product product = Product.builder().id("p1").name("Monitor").price(299.0).quantity(3)
                .sellerId(SELLER_ID).sellerName("Seller").category("Peripherals").imageUrls(List.of()).build();

        when(productRepository.findBySellerId(SELLER_ID)).thenReturn(List.of(product));

        List<ProductDto> result = productService.getMyProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSellerId()).isEqualTo(SELLER_ID);
    }

    @Test
    void getDistinctCategories_returnsSorted() {
        when(mongoTemplate.findDistinct("category", Product.class, String.class))
                .thenReturn(List.of("Electronics", "Clothing", "Books"));

        List<String> result = productService.getDistinctCategories();

        assertThat(result).containsExactly("Books", "Clothing", "Electronics");
    }

    @Test
    void deleteProduct_owner_success() {
        Product product = Product.builder()
                .id("prod-1").name("Widget").price(5.0).quantity(5)
                .sellerId(SELLER_ID).sellerName("Seller").imageUrls(List.of()).build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThatCode(() -> productService.deleteProduct("prod-1")).doesNotThrowAnyException();
        verify(productRepository).deleteById("prod-1");
    }
}
