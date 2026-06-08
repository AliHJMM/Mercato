package com.buyapp.order.controller;

import com.buyapp.order.dto.*;
import com.buyapp.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<OrderDto> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderDto>> getMyOrders(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getMyOrders(status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/redo")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<OrderDto> redoOrder(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.redoOrder(id));
    }

    @PutMapping("/{id}/advance")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<OrderDto> advanceOrderStatus(@PathVariable String id) {
        return ResponseEntity.ok(orderService.advanceOrderStatus(id));
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<OrderDto>> getSellerOrders() {
        return ResponseEntity.ok(orderService.getSellerOrders());
    }

    @GetMapping("/analytics/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<UserAnalyticsDto> getUserAnalytics() {
        return ResponseEntity.ok(orderService.getUserAnalytics());
    }

    @GetMapping("/analytics/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerAnalyticsDto> getSellerAnalytics() {
        return ResponseEntity.ok(orderService.getSellerAnalytics());
    }
}
