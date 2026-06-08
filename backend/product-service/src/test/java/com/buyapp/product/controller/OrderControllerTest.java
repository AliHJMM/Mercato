package com.buyapp.product.controller;

import com.buyapp.product.dto.OrderDto;
import com.buyapp.product.dto.PlaceOrderRequest;
import com.buyapp.product.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "CLIENT")
    void getMyOrders_returnsOrders() throws Exception {
        OrderDto order = OrderDto.builder()
                .id("order-1").buyerId("buyer-1")
                .items(List.of()).total(25.0)
                .status("PLACED").createdAt(LocalDateTime.now()).build();

        when(orderService.getMyOrders(null)).thenReturn(List.of(order));

        mockMvc.perform(get("/orders/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("order-1"))
                .andExpect(jsonPath("$[0].status").value("PLACED"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void getMyOrders_withStatusFilter() throws Exception {
        when(orderService.getMyOrders("PLACED")).thenReturn(List.of());

        mockMvc.perform(get("/orders/my").param("status", "PLACED"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void cancelOrder_returns200() throws Exception {
        OrderDto cancelled = OrderDto.builder()
                .id("order-1").buyerId("buyer-1")
                .items(List.of()).total(25.0)
                .status("CANCELLED").createdAt(LocalDateTime.now()).build();

        when(orderService.cancelOrder("order-1")).thenReturn(cancelled);

        mockMvc.perform(put("/orders/order-1/cancel").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void deleteOrder_returns204() throws Exception {
        mockMvc.perform(delete("/orders/order-1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void getSellerOrders_returnsList() throws Exception {
        when(orderService.getSellerOrders()).thenReturn(List.of());

        mockMvc.perform(get("/orders/seller"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/orders/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void placeOrder_withValidBody_returns201() throws Exception {
        OrderDto placed = OrderDto.builder()
                .id("order-1").buyerId("buyer-1")
                .items(List.of()).total(20.0)
                .status("PLACED").createdAt(LocalDateTime.now()).build();

        when(orderService.placeOrder(any())).thenReturn(placed);

        Map<String, Object> body = Map.of(
                "items", List.of(Map.of("productId", "prod-1", "quantity", 2)),
                "paymentMethod", "PAY_ON_DELIVERY",
                "deliveryAddress", Map.of(
                        "fullName", "John Doe",
                        "street", "123 Main St",
                        "city", "Springfield",
                        "state", "IL",
                        "zipCode", "62701",
                        "country", "US",
                        "phone", "+1555000111"
                )
        );

        mockMvc.perform(post("/orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order-1"))
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void placeOrder_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void placeOrder_asSeller_returns403() throws Exception {
        Map<String, Object> body = Map.of(
                "items", List.of(Map.of("productId", "prod-1", "quantity", 1)),
                "paymentMethod", "PAY_ON_DELIVERY",
                "deliveryAddress", Map.of(
                        "fullName", "Jane", "street", "1 St", "city", "NY",
                        "state", "NY", "zipCode", "10001", "country", "US", "phone", "+1"
                )
        );

        mockMvc.perform(post("/orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void advanceOrderStatus_asSeller_returns200() throws Exception {
        OrderDto advanced = OrderDto.builder()
                .id("order-1").buyerId("buyer-1")
                .items(List.of()).total(25.0)
                .status("CONFIRMED").createdAt(LocalDateTime.now()).build();

        when(orderService.advanceOrderStatus("order-1")).thenReturn(advanced);

        mockMvc.perform(put("/orders/order-1/advance").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void advanceOrderStatus_asClient_returns403() throws Exception {
        mockMvc.perform(put("/orders/order-1/advance").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void getSellerOrders_asClient_returns403() throws Exception {
        mockMvc.perform(get("/orders/seller"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void cancelOrder_asSeller_returns403() throws Exception {
        mockMvc.perform(put("/orders/order-1/cancel").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void deleteOrder_asSeller_returns403() throws Exception {
        mockMvc.perform(delete("/orders/order-1").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
