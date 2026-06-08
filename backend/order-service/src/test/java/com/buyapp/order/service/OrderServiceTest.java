package com.buyapp.order.service;

import com.buyapp.order.client.ProductClient;
import com.buyapp.order.client.ProductInfo;
import com.buyapp.order.client.StockRequest;
import com.buyapp.order.dto.*;
import com.buyapp.order.entity.*;
import com.buyapp.order.exception.AppException;
import com.buyapp.order.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    private static final String BUYER_ID = "buyer-123";
    private static final String SELLER_ID = "seller-456";
    private static final String PRODUCT_ID = "product-789";
    private static final String ORDER_ID = "order-001";

    @BeforeEach
    void setUp() {
        setClientAuth();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setClientAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(BUYER_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))));
    }

    private void setSellerAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SELLER_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_SELLER"))));
    }

    private ProductInfo sampleProduct() {
        ProductInfo p = new ProductInfo();
        p.setId(PRODUCT_ID);
        p.setName("Test Product");
        p.setPrice(50.0);
        p.setQuantity(10);
        p.setSellerId(SELLER_ID);
        p.setSellerName("Test Seller");
        p.setImageUrls(List.of("http://img.example.com/1.jpg"));
        p.setCategory("Electronics");
        return p;
    }

    private Order sampleOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .buyerId(BUYER_ID)
                .status(OrderStatus.PLACED)
                .total(100.0)
                .items(List.of(OrderItem.builder()
                        .productId(PRODUCT_ID)
                        .productName("Test Product")
                        .sellerId(SELLER_ID)
                        .price(50.0)
                        .quantity(2)
                        .subtotal(100.0)
                        .build()))
                .build();
    }

    private PlaceOrderRequest samplePlaceRequest() {
        PlaceOrderRequest req = new PlaceOrderRequest();

        PlaceOrderRequest.OrderItemRequest item = new PlaceOrderRequest.OrderItemRequest();
        item.setProductId(PRODUCT_ID);
        item.setQuantity(2);
        req.setItems(List.of(item));
        req.setPaymentMethod("PAY_ON_DELIVERY");

        PlaceOrderRequest.DeliveryAddressRequest addr = new PlaceOrderRequest.DeliveryAddressRequest();
        addr.setFullName("John Doe");
        addr.setStreet("123 Main St");
        addr.setCity("New York");
        addr.setState("NY");
        addr.setZipCode("10001");
        addr.setCountry("USA");
        addr.setPhone("555-0100");
        req.setDeliveryAddress(addr);

        return req;
    }

    @Test
    void placeOrder_success_returnsOrderDto() {
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(sampleProduct());
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return Order.builder().id(ORDER_ID).buyerId(o.getBuyerId())
                    .items(o.getItems()).total(o.getTotal()).status(o.getStatus())
                    .paymentMethod(o.getPaymentMethod()).deliveryAddress(o.getDeliveryAddress()).build();
        });

        OrderDto result = orderService.placeOrder(samplePlaceRequest());

        assertThat(result.getBuyerId()).isEqualTo(BUYER_ID);
        assertThat(result.getTotal()).isEqualTo(100.0);
        assertThat(result.getStatus()).isEqualTo("PLACED");
        verify(productClient).reduceStock(anyList());
    }

    @Test
    void placeOrder_insufficientStock_throws409() {
        ProductInfo product = sampleProduct();
        product.setQuantity(1);

        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product);

        PlaceOrderRequest req = samplePlaceRequest();
        req.getItems().get(0).setQuantity(5);

        assertThatThrownBy(() -> orderService.placeOrder(req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Insufficient stock");

        verify(productClient, never()).reduceStock(anyList());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_invalidPaymentMethod_defaultsToPAY_ON_DELIVERY() {
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(sampleProduct());
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return Order.builder().id(ORDER_ID).buyerId(o.getBuyerId())
                    .items(o.getItems()).total(o.getTotal()).status(o.getStatus())
                    .paymentMethod(o.getPaymentMethod()).build();
        });

        PlaceOrderRequest req = samplePlaceRequest();
        req.setPaymentMethod("BITCOIN");

        OrderDto result = orderService.placeOrder(req);

        assertThat(result.getPaymentMethod()).isEqualTo("PAY_ON_DELIVERY");
    }

    @Test
    void getMyOrders_noStatus_returnsAll() {
        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(BUYER_ID))
                .thenReturn(List.of(sampleOrder()));

        List<OrderDto> result = orderService.getMyOrders(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBuyerId()).isEqualTo(BUYER_ID);
    }

    @Test
    void getMyOrders_withStatus_returnsFiltered() {
        when(orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(BUYER_ID, OrderStatus.PLACED))
                .thenReturn(List.of(sampleOrder()));

        List<OrderDto> result = orderService.getMyOrders("PLACED");

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyOrders_invalidStatus_throws400() {
        assertThatThrownBy(() -> orderService.getMyOrders("INVALID"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getOrderById_buyerAccess_success() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(sampleOrder()));

        OrderDto result = orderService.getOrderById(ORDER_ID);

        assertThat(result.getId()).isEqualTo(ORDER_ID);
    }

    @Test
    void getOrderById_notFound_throws404() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteOrder_placedOrder_success() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(sampleOrder()));

        orderService.deleteOrder(ORDER_ID);

        verify(orderRepository).deleteById(ORDER_ID);
        verify(productClient).restoreStock(anyList());
    }

    @Test
    void deleteOrder_notOwnedByBuyer_throws403() {
        Order order = sampleOrder();
        order.setBuyerId("other-user");
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.deleteOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void advanceOrderStatus_placedToConfirmed_success() {
        setSellerAuth();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(sampleOrder()));
        when(orderRepository.save(any())).thenReturn(sampleOrder());

        OrderDto result = orderService.advanceOrderStatus(ORDER_ID);

        verify(orderRepository).save(any());
    }

    @Test
    void advanceOrderStatus_notSeller_throws403() {
        setSellerAuth();
        Order order = sampleOrder();
        order.getItems().get(0).setSellerId("different-seller");
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.advanceOrderStatus(ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void advanceOrderStatus_deliveredOrder_throwsConflict() {
        setSellerAuth();
        Order order = sampleOrder();
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.advanceOrderStatus(ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getSellerOrders_returnsSellersOrders() {
        setSellerAuth();
        when(orderRepository.findOrdersContainingSeller(SELLER_ID)).thenReturn(List.of(sampleOrder()));

        List<OrderDto> result = orderService.getSellerOrders();

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserAnalytics_calculatesCorrectTotals() {
        Order order = sampleOrder();
        order.getItems().get(0).setCategory("Electronics");
        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(BUYER_ID)).thenReturn(List.of(order));

        UserAnalyticsDto result = orderService.getUserAnalytics();

        assertThat(result.getTotalOrders()).isEqualTo(1);
        assertThat(result.getTotalSpent()).isEqualTo(100.0);
        assertThat(result.getMostBoughtProducts()).hasSize(1);
        assertThat(result.getTopCategories()).hasSize(1);
    }

    @Test
    void getUserAnalytics_cancelledOrdersExcluded() {
        Order cancelled = sampleOrder();
        cancelled.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(BUYER_ID)).thenReturn(List.of(cancelled));

        UserAnalyticsDto result = orderService.getUserAnalytics();

        assertThat(result.getTotalOrders()).isEqualTo(0);
        assertThat(result.getTotalSpent()).isEqualTo(0.0);
    }

    @Test
    void getSellerAnalytics_calculatesCorrectTotals() {
        setSellerAuth();
        Order order = sampleOrder();
        when(orderRepository.findOrdersContainingSeller(SELLER_ID)).thenReturn(List.of(order));

        SellerAnalyticsDto result = orderService.getSellerAnalytics();

        assertThat(result.getTotalOrdersProcessed()).isEqualTo(1);
        assertThat(result.getTotalRevenue()).isEqualTo(100.0);
        assertThat(result.getTotalUnitsSold()).isEqualTo(2);
    }
}
