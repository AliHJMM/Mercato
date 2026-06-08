package com.buyapp.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
    }

    private MockServerWebExchange authExchange(String ip) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .header("X-Forwarded-For", ip)
                        .build());
    }

    private MockServerWebExchange mediaUploadExchange(String ip) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/media/images")
                        .header("X-Forwarded-For", ip)
                        .build());
    }

    private MockServerWebExchange productExchange(String ip) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .header("X-Forwarded-For", ip)
                        .build());
    }

    private GatewayFilterChain chainThatCompletes() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        return chain;
    }

    @Test
    void firstAuthRequest_passes() {
        GatewayFilterChain chain = chainThatCompletes();
        var exchange = authExchange("10.0.0.1");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void productRequest_noLimit_alwaysPasses() {
        GatewayFilterChain chain = chainThatCompletes();

        for (int i = 0; i < 50; i++) {
            var exchange = productExchange("10.0.0.2");
            filter.filter(exchange, chain).block();
        }

        verify(chain, times(50)).filter(any());
    }

    @Test
    void authEndpoint_differentIPs_separateLimits() {
        GatewayFilterChain chain = chainThatCompletes();

        // First IP sends 1 request
        var ex1 = authExchange("192.168.1.1");
        filter.filter(ex1, chain).block();

        // Second IP sends 1 request — should pass independently
        var ex2 = authExchange("192.168.1.2");
        filter.filter(ex2, chain).block();

        verify(chain, times(2)).filter(any());
        assertThat(ex2.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void authEndpoint_exceedsLimit_returns429() {
        GatewayFilterChain chain = chainThatCompletes();
        String ip = "10.0.0.3";

        // Send 10 requests (the limit for /api/auth/**)
        for (int i = 0; i < 10; i++) {
            filter.filter(authExchange(ip), chain).block();
        }

        // 11th request should be rate-limited
        var overLimitExchange = authExchange(ip);
        filter.filter(overLimitExchange, chain).block();

        assertThat(overLimitExchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void mediaUpload_exceedsLimit_returns429() {
        GatewayFilterChain chain = chainThatCompletes();
        String ip = "10.0.0.4";

        // Send 20 requests (the limit for POST /api/media/**)
        for (int i = 0; i < 20; i++) {
            filter.filter(mediaUploadExchange(ip), chain).block();
        }

        // 21st should be rate-limited
        var overLimitExchange = mediaUploadExchange(ip);
        filter.filter(overLimitExchange, chain).block();

        assertThat(overLimitExchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void rateLimitFilter_order_isNegative() {
        assertThat(filter.getOrder()).isLessThan(0);
    }
}
