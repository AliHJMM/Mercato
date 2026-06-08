package com.buyapp.product.service;

import com.buyapp.product.dto.OrderDto;
import com.buyapp.product.dto.PlaceOrderRequest;
import com.buyapp.product.entity.*;
import com.buyapp.product.exception.AppException;
import com.buyapp.product.repository.OrderRepository;
import com.buyapp.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private static final String BUYER_ID = "buyer-001";
    private static final String SELLER_ID = "seller-001";

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken(
                BUYER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void placeOrder_success() {
        Product product = Product.builder()
                .id("prod-1").name("Widget").price(10.0).quantity(5)
                .sellerId(SELLER_ID).sellerName("Seller").category("Electronics")
                .imageUrls(List.of()).build();

        PlaceOrderRequest.OrderItemRequest itemReq = new PlaceOrderRequest.OrderItemRequest();
        itemReq.setProductId("prod-1");
        itemReq.setQuantity(2);

        PlaceOrderRequest.DeliveryAddressRequest addr = new PlaceOrderRequest.DeliveryAddressRequest();
        addr.setFullName("John Doe");
        addr.setStreet("123 Main St");
        addr.setCity("Springfield");
        addr.setState("IL");
        addr.setZipCode("62701");
        addr.setCountry("US");
        addr.setPhone("+1555000111");

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setItems(List.of(itemReq));
        request.setDeliveryAddress(addr);
        request.setPaymentMethod("PAY_ON_DELIVERY");

        Order savedOrder = Order.builder()
                .id("order-1").buyerId(BUYER_ID)
                .items(List.of(OrderItem.builder()
                        .productId("prod-1").productName("Widget")
                        .sellerId(SELLER_ID).sellerName("Seller")
                        .price(10.0).quantity(2).subtotal(20.0).build()))
                .total(20.0).status(OrderStatus.PLACED)
                .createdAt(LocalDateTime.now()).build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any())).thenReturn(savedOrder);

        OrderDto result = orderService.placeOrder(request);

        assertThat(result.getTotal()).isEqualTo(20.0);
        assertThat(result.getStatus()).isEqualTo("PLACED");
        verify(productRepository).save(argThat(p -> p.getQuantity() == 3));
    }

    @Test
    void placeOrder_insufficientStock_throws() {
        Product product = Product.builder()
                .id("prod-1").name("Widget").price(10.0).quantity(1)
                .sellerId(SELLER_ID).sellerName("Seller").imageUrls(List.of()).build();

        PlaceOrderRequest.OrderItemRequest itemReq = new PlaceOrderRequest.OrderItemRequest();
        itemReq.setProductId("prod-1");
        itemReq.setQuantity(5);

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setItems(List.of(itemReq));
        request.setDeliveryAddress(new PlaceOrderRequest.DeliveryAddressRequest());

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void cancelOrder_placed_success() {
        Order order = Order.builder()
                .id("order-1").buyerId(BUYER_ID)
                .items(List.of(OrderItem.builder()
                        .productId("prod-1").quantity(2).subtotal(20.0).build()))
                .total(20.0).status(OrderStatus.PLACED).build();

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(
                Product.builder().id("prod-1").quantity(3).imageUrls(List.of()).build()));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OrderDto result = orderService.cancelOrder("order-1");

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelOrder_delivered_throws() {
        Order order = Order.builder()
                .id("order-1").buyerId(BUYER_ID).items(List.of())
                .total(20.0).status(OrderStatus.DELIVERED).build();

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("order-1"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    @Test
    void deleteOrder_notPlaced_throws() {
        Order order = Order.builder()
                .id("order-1").buyerId(BUYER_ID).items(List.of())
                .total(10.0).status(OrderStatus.SHIPPED).build();

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.deleteOrder("order-1"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("PLACED status");
    }

    @Test
    void redoOrder_cancelled_success() {
        Order original = Order.builder()
                .id("order-1").buyerId(BUYER_ID)
                .items(List.of(OrderItem.builder()
                        .productId("prod-1").quantity(1).subtotal(10.0).build()))
                .total(10.0).status(OrderStatus.CANCELLED)
                .deliveryAddress(DeliveryAddress.builder()
                        .fullName("John").street("Main St").city("NY").state("NY")
                        .zipCode("10001").country("US").phone("+1555").build())
                .build();

        Product product = Product.builder()
                .id("prod-1").name("Widget").price(10.0).quantity(5)
                .sellerId(SELLER_ID).sellerName("Seller").imageUrls(List.of()).build();

        Order savedRedo = Order.builder()
                .id("order-2").buyerId(BUYER_ID)
                .items(original.getItems())
                .total(10.0).status(OrderStatus.PLACED)
                .createdAt(LocalDateTime.now()).build();

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(original));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any())).thenReturn(savedRedo);

        OrderDto result = orderService.redoOrder("order-1");

        assertThat(result.getStatus()).isEqualTo("PLACED");
    }

    @Test
    void cancelOrder_notOwner_throws() {
        Order order = Order.builder()
                .id("order-1").buyerId("different-buyer").items(List.of())
                .total(10.0).status(OrderStatus.PLACED).build();

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("order-1"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("do not own");
    }
}
