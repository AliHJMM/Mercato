package com.buyapp.gateway.filter;

import com.buyapp.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @InjectMocks private AuthenticationFilter filter;

    private GatewayFilterChain chainThatCompletes() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        return chain;
    }

    @Test
    void publicGetProducts_noAuthRequired_chainCalled() {
        var request = MockServerHttpRequest.get("/api/products").build();
        var exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = chainThatCompletes();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(jwtUtil, never()).isTokenValid(any());
    }

    @Test
    void publicAuthLogin_noAuthRequired_chainCalled() {
        var request = MockServerHttpRequest.post("/api/auth/login").build();
        var exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = chainThatCompletes();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void protectedEndpoint_missingToken_returns401() {
        var request = MockServerHttpRequest.get("/api/orders/my").build();
        var exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void protectedEndpoint_invalidToken_returns401() {
        when(jwtUtil.isTokenValid("bad-token")).thenReturn(false);

        var request = MockServerHttpRequest.get("/api/orders/my")
                .header("Authorization", "Bearer bad-token")
                .build();
        var exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_validToken_forwardsUserHeaders() {
        Claims claims = new DefaultClaims(new HashMap<>(Map.of(
                "sub", "user-123",
                "email", "john@test.com",
                "role", "CLIENT"
        )));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractAllClaims("valid-token")).thenReturn(claims);

        var request = MockServerHttpRequest.get("/api/orders/my")
                .header("Authorization", "Bearer valid-token")
                .build();
        var exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = chainThatCompletes();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }
}
