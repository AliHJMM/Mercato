package com.buyapp.product.service;

import com.buyapp.product.dto.*;
import com.buyapp.product.entity.*;
import com.buyapp.product.entity.PaymentMethod;
import com.buyapp.product.exception.AppException;
import com.buyapp.product.repository.OrderRepository;
import com.buyapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
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
                    .sellerId(product.getSellerId())
                    .sellerName(product.getSellerName())
                    .imageUrl(imageUrl)
                    .category(product.getCategory())
                    .price(product.getPrice())
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build());
        }

        DeliveryAddress address = null;
        if (request.getDeliveryAddress() != null) {
            PlaceOrderRequest.DeliveryAddressRequest a = request.getDeliveryAddress();
            address = DeliveryAddress.builder()
                    .fullName(a.getFullName())
                    .street(a.getStreet())
                    .city(a.getCity())
                    .state(a.getState())
                    .zipCode(a.getZipCode())
                    .country(a.getCountry())
                    .phone(a.getPhone())
                    .build();
        }

        PaymentMethod paymentMethod = PaymentMethod.PAY_ON_DELIVERY;
        if (request.getPaymentMethod() != null) {
            try { paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod()); }
            catch (IllegalArgumentException ignored) {}
        }

        Order order = Order.builder()
                .buyerId(buyerId)
                .items(items)
                .total(total)
                .status(OrderStatus.PLACED)
                .paymentMethod(paymentMethod)
                .deliveryAddress(address)
                .build();

        order = orderRepository.save(order);
        log.info("Order {} placed by buyer {}", order.getId(), buyerId);

        return OrderDto.from(order);
    }

    public List<OrderDto> getMyOrders(String status) {
        String buyerId = getCurrentUserId();

        if (status != null && !status.isBlank()) {
            OrderStatus orderStatus = parseStatus(status);
            return orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyerId, orderStatus)
                    .stream().map(OrderDto::from).toList();
        }

        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream().map(OrderDto::from).toList();
    }

    public OrderDto getOrderById(String id) {
        Order order = findOrderById(id);
        enforceOrderAccess(order);
        return OrderDto.from(order);
    }

    public OrderDto cancelOrder(String id) {
        Order order = findOrderById(id);
        enforceOrderOwnership(order);

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new AppException(
                    "Order cannot be cancelled. Only PLACED or CONFIRMED orders can be cancelled.",
                    HttpStatus.CONFLICT
            );
        }

        restoreStock(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order {} cancelled by buyer {}", id, getCurrentUserId());
        return OrderDto.from(order);
    }

    public void deleteOrder(String id) {
        Order order = findOrderById(id);
        enforceOrderOwnership(order);

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new AppException(
                    "Order can only be deleted when in PLACED status.",
                    HttpStatus.CONFLICT
            );
        }

        restoreStock(order);
        orderRepository.deleteById(id);
        log.info("Order {} deleted by buyer {}", id, getCurrentUserId());
    }

    public OrderDto redoOrder(String id) {
        Order original = findOrderById(id);
        enforceOrderOwnership(original);

        if (original.getStatus() != OrderStatus.CANCELLED && original.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException(
                    "Only CANCELLED or DELIVERED orders can be re-ordered.",
                    HttpStatus.CONFLICT
            );
        }

        PlaceOrderRequest request = new PlaceOrderRequest();
        List<PlaceOrderRequest.OrderItemRequest> itemRequests = original.getItems().stream()
                .map(item -> {
                    PlaceOrderRequest.OrderItemRequest r = new PlaceOrderRequest.OrderItemRequest();
                    r.setProductId(item.getProductId());
                    r.setQuantity(item.getQuantity());
                    return r;
                })
                .collect(Collectors.toList());
        request.setItems(itemRequests);
        request.setPaymentMethod(original.getPaymentMethod() != null
                ? original.getPaymentMethod().name() : PaymentMethod.PAY_ON_DELIVERY.name());

        if (original.getDeliveryAddress() != null) {
            DeliveryAddress a = original.getDeliveryAddress();
            PlaceOrderRequest.DeliveryAddressRequest addr = new PlaceOrderRequest.DeliveryAddressRequest();
            addr.setFullName(a.getFullName());
            addr.setStreet(a.getStreet());
            addr.setCity(a.getCity());
            addr.setState(a.getState());
            addr.setZipCode(a.getZipCode());
            addr.setCountry(a.getCountry());
            addr.setPhone(a.getPhone());
            request.setDeliveryAddress(addr);
        }

        return placeOrder(request);
    }

    public OrderDto advanceOrderStatus(String id) {
        String sellerId = getCurrentUserId();
        Order order = findOrderById(id);

        boolean isSeller = order.getItems().stream()
                .anyMatch(item -> sellerId.equals(item.getSellerId()));
        if (!isSeller) {
            throw new AppException("You do not have products in this order", HttpStatus.FORBIDDEN);
        }

        OrderStatus next = switch (order.getStatus()) {
            case PLACED    -> OrderStatus.CONFIRMED;
            case CONFIRMED -> OrderStatus.SHIPPED;
            case SHIPPED   -> OrderStatus.DELIVERED;
            default -> throw new AppException(
                    "Order status '" + order.getStatus() + "' cannot be advanced.", HttpStatus.CONFLICT);
        };

        order.setStatus(next);
        orderRepository.save(order);
        log.info("Order {} advanced to {} by seller {}", id, next, sellerId);
        return OrderDto.from(order);
    }

    public List<OrderDto> getSellerOrders() {
        String sellerId = getCurrentUserId();
        return orderRepository.findOrdersContainingSeller(sellerId)
                .stream().map(OrderDto::from).toList();
    }

    public UserAnalyticsDto getUserAnalytics() {
        String userId = getCurrentUserId();
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(userId);

        double totalSpent = 0;
        int totalOrders = 0;
        Map<String, int[]> productQty = new LinkedHashMap<>();
        Map<String, double[]> productSpent = new LinkedHashMap<>();
        Map<String, String> productNames = new LinkedHashMap<>();
        Map<String, double[]> categorySpent = new LinkedHashMap<>();
        Map<String, int[]> categoryItems = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED) continue;
            totalSpent += order.getTotal();
            totalOrders++;

            for (OrderItem item : order.getItems()) {
                String pid = item.getProductId();
                productQty.computeIfAbsent(pid, k -> new int[]{0})[0] += item.getQuantity();
                productSpent.computeIfAbsent(pid, k -> new double[]{0.0})[0] += item.getSubtotal();
                productNames.putIfAbsent(pid, item.getProductName());

                String cat = item.getCategory() != null ? item.getCategory() : "Uncategorized";
                categorySpent.computeIfAbsent(cat, k -> new double[]{0.0})[0] += item.getSubtotal();
                categoryItems.computeIfAbsent(cat, k -> new int[]{0})[0] += item.getQuantity();
            }
        }

        List<UserAnalyticsDto.ProductPurchaseDto> mostBought = productQty.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0] - a.getValue()[0])
                .limit(5)
                .map(e -> UserAnalyticsDto.ProductPurchaseDto.builder()
                        .productId(e.getKey())
                        .productName(productNames.get(e.getKey()))
                        .totalQuantity(e.getValue()[0])
                        .totalSpent(productSpent.get(e.getKey())[0])
                        .build())
                .collect(Collectors.toList());

        List<UserAnalyticsDto.CategorySpendDto> topCategories = categorySpent.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .map(e -> UserAnalyticsDto.CategorySpendDto.builder()
                        .category(e.getKey())
                        .totalSpent(e.getValue()[0])
                        .totalItems(categoryItems.get(e.getKey())[0])
                        .build())
                .collect(Collectors.toList());

        return UserAnalyticsDto.builder()
                .totalSpent(totalSpent)
                .totalOrders(totalOrders)
                .mostBoughtProducts(mostBought)
                .topCategories(topCategories)
                .build();
    }

    public SellerAnalyticsDto getSellerAnalytics() {
        String sellerId = getCurrentUserId();
        List<Order> orders = orderRepository.findOrdersContainingSeller(sellerId);

        double totalRevenue = 0;
        int totalOrdersProcessed = 0;
        int totalUnitsSold = 0;
        Map<String, int[]> productUnits = new LinkedHashMap<>();
        Map<String, double[]> productRevenue = new LinkedHashMap<>();
        Map<String, String> productNames = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED) continue;
            boolean hasSellerItems = false;

            for (OrderItem item : order.getItems()) {
                if (!sellerId.equals(item.getSellerId())) continue;
                totalRevenue += item.getSubtotal();
                totalUnitsSold += item.getQuantity();
                hasSellerItems = true;

                String pid = item.getProductId();
                productUnits.computeIfAbsent(pid, k -> new int[]{0})[0] += item.getQuantity();
                productRevenue.computeIfAbsent(pid, k -> new double[]{0.0})[0] += item.getSubtotal();
                productNames.putIfAbsent(pid, item.getProductName());
            }

            if (hasSellerItems) totalOrdersProcessed++;
        }

        List<SellerAnalyticsDto.ProductSalesDto> bestSelling = productUnits.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0] - a.getValue()[0])
                .limit(5)
                .map(e -> SellerAnalyticsDto.ProductSalesDto.builder()
                        .productId(e.getKey())
                        .productName(productNames.get(e.getKey()))
                        .unitsSold(e.getValue()[0])
                        .revenue(productRevenue.get(e.getKey())[0])
                        .build())
                .collect(Collectors.toList());

        return SellerAnalyticsDto.builder()
                .totalRevenue(totalRevenue)
                .totalOrdersProcessed(totalOrdersProcessed)
                .totalUnitsSold(totalUnitsSold)
                .bestSellingProducts(bestSelling)
                .build();
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setQuantity(product.getQuantity() + item.getQuantity());
                productRepository.save(product);
            });
        }
    }

    private Order findOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new AppException("Order not found", HttpStatus.NOT_FOUND));
    }

    private void enforceOrderOwnership(Order order) {
        String userId = getCurrentUserId();
        if (!order.getBuyerId().equals(userId)) {
            throw new AppException("You do not own this order", HttpStatus.FORBIDDEN);
        }
    }

    private void enforceOrderAccess(Order order) {
        String userId = getCurrentUserId();
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getItems().stream()
                .anyMatch(item -> userId.equals(item.getSellerId()));
        if (!isBuyer && !isSeller) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException("Invalid status: " + status, HttpStatus.BAD_REQUEST);
        }
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
