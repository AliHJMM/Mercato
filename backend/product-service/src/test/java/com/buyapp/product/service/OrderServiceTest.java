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

import com.buyapp.product.dto.SellerAnalyticsDto;
import com.buyapp.product.dto.UserAnalyticsDto;

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

    @Test
    void getMyOrders_noStatus_returnsAllOrders() {
        Order order = Order.builder().id("o1").buyerId(BUYER_ID).items(List.of())
                .total(10.0).status(OrderStatus.PLACED).createdAt(LocalDateTime.now()).build();

        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(BUYER_ID)).thenReturn(List.of(order));

        List<OrderDto> result = orderService.getMyOrders(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("o1");
    }

    @Test
    void getMyOrders_withStatus_returnsFiltered() {
        Order order = Order.builder().id("o1").buyerId(BUYER_ID).items(List.of())
                .total(10.0).status(OrderStatus.DELIVERED).createdAt(LocalDateTime.now()).build();

        when(orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(BUYER_ID, OrderStatus.DELIVERED))
                .thenReturn(List.of(order));

        List<OrderDto> result = orderService.getMyOrders("DELIVERED");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void getMyOrders_invalidStatus_throws400() {
        assertThatThrownBy(() -> orderService.getMyOrders("INVALID_STATUS"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    void getOrderById_success() {
        Order order = Order.builder().id("o1").buyerId(BUYER_ID)
                .items(List.of()).total(10.0).status(OrderStatus.PLACED)
                .createdAt(LocalDateTime.now()).build();

        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        OrderDto result = orderService.getOrderById("o1");

        assertThat(result.getId()).isEqualTo("o1");
    }

    @Test
    void getOrderById_notFound_throws404() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("missing"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteOrder_placed_success() {
        Order order = Order.builder().id("o1").buyerId(BUYER_ID)
                .items(List.of(OrderItem.builder().productId("p1").quantity(1).subtotal(10.0).build()))
                .total(10.0).status(OrderStatus.PLACED).build();

        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(productRepository.findById("p1")).thenReturn(Optional.of(
                Product.builder().id("p1").quantity(2).imageUrls(List.of()).build()));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.deleteOrder("o1");

        verify(orderRepository).deleteById("o1");
    }

    @Test
    void advanceOrderStatus_placed_becomesConfirmed() {
        setSellerAuth();
        Order order = Order.builder().id("o1").buyerId(BUYER_ID)
                .items(List.of(OrderItem.builder().sellerId(SELLER_ID).productId("p1")
                        .quantity(1).subtotal(10.0).build()))
                .total(10.0).status(OrderStatus.PLACED).build();

        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OrderDto result = orderService.advanceOrderStatus("o1");

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void advanceOrderStatus_notSeller_throws403() {
        setSellerAuth();
        Order order = Order.builder().id("o1").buyerId(BUYER_ID)
                .items(List.of(OrderItem.builder().sellerId("other-seller").productId("p1")
                        .quantity(1).subtotal(10.0).build()))
                .total(10.0).status(OrderStatus.PLACED).build();

        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.advanceOrderStatus("o1"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("do not have products");
    }

    @Test
    void advanceOrderStatus_delivered_throws() {
        setSellerAuth();
        Order order = Order.builder().id("o1").buyerId(BUYER_ID)
                .items(List.of(OrderItem.builder().sellerId(SELLER_ID).productId("p1")
                        .quantity(1).subtotal(10.0).build()))
                .total(10.0).status(OrderStatus.DELIVERED).build();

        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.advanceOrderStatus("o1"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("cannot be advanced");
    }

    @Test
    void getSellerOrders_returnsList() {
        setSellerAuth();
        Order order = Order.builder().id("o1").buyerId(BUYER_ID)
                .items(List.of(OrderItem.builder().sellerId(SELLER_ID).productId("p1")
                        .quantity(1).subtotal(10.0).build()))
                .total(10.0).status(OrderStatus.PLACED).createdAt(LocalDateTime.now()).build();

        when(orderRepository.findOrdersContainingSeller(SELLER_ID)).thenReturn(List.of(order));

        List<OrderDto> result = orderService.getSellerOrders();

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserAnalytics_returnsCorrectTotals() {
        Order order = Order.builder().id("o1").buyerId(BUYER_ID)
                .items(List.of(
                        OrderItem.builder().productId("p1").productName("Widget")
                                .sellerId(SELLER_ID).category("Electronics")
                                .quantity(2).subtotal(20.0).build()))
                .total(20.0).status(OrderStatus.DELIVERED)
                .createdAt(LocalDateTime.now()).build();

        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(BUYER_ID)).thenReturn(List.of(order));

        UserAnalyticsDto result = orderService.getUserAnalytics();

        assertThat(result.getTotalSpent()).isEqualTo(20.0);
        assertThat(result.getTotalOrders()).isEqualTo(1);
        assertThat(result.getMostBoughtProducts()).hasSize(1);
        assertThat(result.getMostBoughtProducts().get(0).getProductName()).isEqualTo("Widget");
        assertThat(result.getTopCategories()).hasSize(1);
        assertThat(result.getTopCategories().get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    void getUserAnalytics_cancelledOrdersExcluded() {
        Order cancelled = Order.builder().id("o1").buyerId(BUYER_ID)
                .items(List.of(OrderItem.builder().productId("p1").productName("Widget")
                        .quantity(1).subtotal(10.0).category("Electronics").build()))
                .total(10.0).status(OrderStatus.CANCELLED).createdAt(LocalDateTime.now()).build();

        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(BUYER_ID)).thenReturn(List.of(cancelled));

        UserAnalyticsDto result = orderService.getUserAnalytics();

        assertThat(result.getTotalSpent()).isEqualTo(0.0);
        assertThat(result.getTotalOrders()).isEqualTo(0);
    }

    @Test
    void getSellerAnalytics_returnsCorrectData() {
        setSellerAuth();
        Order order = Order.builder().id("o1").buyerId(BUYER_ID)
                .items(List.of(
                        OrderItem.builder().productId("p1").productName("Widget")
                                .sellerId(SELLER_ID).quantity(3).subtotal(30.0).build()))
                .total(30.0).status(OrderStatus.DELIVERED)
                .createdAt(LocalDateTime.now()).build();

        when(orderRepository.findOrdersContainingSeller(SELLER_ID)).thenReturn(List.of(order));

        SellerAnalyticsDto result = orderService.getSellerAnalytics();

        assertThat(result.getTotalRevenue()).isEqualTo(30.0);
        assertThat(result.getTotalUnitsSold()).isEqualTo(3);
        assertThat(result.getTotalOrdersProcessed()).isEqualTo(1);
        assertThat(result.getBestSellingProducts()).hasSize(1);
        assertThat(result.getBestSellingProducts().get(0).getProductName()).isEqualTo("Widget");
    }

    @Test
    void placeOrder_invalidPaymentMethod_defaultsToPOD() {
        Product product = Product.builder()
                .id("p1").name("Widget").price(10.0).quantity(5)
                .sellerId(SELLER_ID).sellerName("Seller").imageUrls(List.of()).build();

        PlaceOrderRequest.OrderItemRequest itemReq = new PlaceOrderRequest.OrderItemRequest();
        itemReq.setProductId("p1");
        itemReq.setQuantity(1);

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setItems(List.of(itemReq));
        request.setDeliveryAddress(new PlaceOrderRequest.DeliveryAddressRequest());
        request.setPaymentMethod("INVALID_METHOD");

        Order saved = Order.builder().id("o1").buyerId(BUYER_ID).items(List.of())
                .total(10.0).status(OrderStatus.PLACED)
                .paymentMethod(PaymentMethod.PAY_ON_DELIVERY)
                .createdAt(LocalDateTime.now()).build();

        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any())).thenReturn(saved);

        OrderDto result = orderService.placeOrder(request);

        assertThat(result.getPaymentMethod()).isEqualTo("PAY_ON_DELIVERY");
    }

    private void setSellerAuth() {
        var auth = new UsernamePasswordAuthenticationToken(
                SELLER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
