package com.buyapp.product.controller;

import com.buyapp.product.dto.ProductDto;
import com.buyapp.product.dto.ProductSearchResponse;
import com.buyapp.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getAllProducts_public_returns200() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }

    @Test
    void searchProducts_noParams_returns200() throws Exception {
        ProductSearchResponse response = ProductSearchResponse.builder()
                .products(List.of()).total(0).page(0).size(12).totalPages(0).build();

        when(productService.searchProducts(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/products/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void searchProducts_withKeyword_returns200() throws Exception {
        ProductDto product = ProductDto.builder()
                .id("p1").name("Keyboard").price(29.9).quantity(5)
                .sellerId("s1").sellerName("Seller").category("Peripherals")
                .imageUrls(List.of()).build();

        ProductSearchResponse response = ProductSearchResponse.builder()
                .products(List.of(product)).total(1).page(0).size(12).totalPages(1).build();

        when(productService.searchProducts(eq("keyword"), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/products/search").param("q", "keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].name").value("Keyboard"));
    }

    @Test
    void getCategories_public_returns200() throws Exception {
        when(productService.getDistinctCategories()).thenReturn(List.of("Gaming", "Peripherals"));

        mockMvc.perform(get("/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Gaming"));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void getMyProducts_authenticated_returns200() throws Exception {
        when(productService.getMyProducts()).thenReturn(List.of());
        mockMvc.perform(get("/products/my"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyProducts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/products/my").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }
}
