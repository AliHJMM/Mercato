package com.buyapp.product.service;

import com.buyapp.product.dto.OrderDto;
import com.buyapp.product.dto.PlaceOrderRequest;
import com.buyapp.product.entity.Order;
import com.buyapp.product.entity.OrderItem;
import com.buyapp.product.entity.Product;
import com.buyapp.product.exception.AppException;
import com.buyapp.product.repository.OrderRepository;
import com.buyapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderDto placeOrder(PlaceOrderRequest request) {
        String buyerId = getCurrentUserId();
        List<OrderItem> items = new ArrayList<>();
        double total = 0;

        for (PlaceOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new AppException("Product not found: " + itemReq.getProductId(), HttpStatus.NOT_FOUND));

            if (product.getQuantity() < itemReq.getQuantity()) {
                throw new AppException(
                        "Insufficient stock for \"" + product.getName() + "\". Available: " + product.getQuantity(),
                        HttpStatus.CONFLICT
                );
            }

            // Deduct stock
            product.setQuantity(product.getQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            double subtotal = product.getPrice() * itemReq.getQuantity();
            total += subtotal;

            String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                    ? product.getImageUrls().get(0)
                    : null;

            items.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .sellerName(product.getSellerName())
                    .imageUrl(imageUrl)
                    .price(product.getPrice())
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build());
        }

        Order order = Order.builder()
                .buyerId(buyerId)
                .items(items)
                .total(total)
                .status("PLACED")
                .build();

        order = orderRepository.save(order);
        log.info("Order {} placed by buyer {}", order.getId(), buyerId);

        return OrderDto.from(order);
    }

    public List<OrderDto> getMyOrders() {
        String buyerId = getCurrentUserId();
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream()
                .map(OrderDto::from)
                .collect(Collectors.toList());
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
