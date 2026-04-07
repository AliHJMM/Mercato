package com.buyapp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter implements GlobalFilter, Ordered {

    // Per-IP sliding-window request timestamp queues
    private final ConcurrentHashMap<String, Deque<Long>> windowMap = new ConcurrentHashMap<>();

    private static final long WINDOW_MS = 60_000L; // 1 minute window

    // Requests allowed per minute per IP per bucket
    private static final int AUTH_RPM    = 10;  // login / register
    private static final int UPLOAD_RPM  = 20;  // media image upload

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path   = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        String ip     = resolveClientIp(exchange);

        int limit = resolveLimit(path, method);
        if (limit > 0 && isRateLimited(ip + ":" + bucketKey(path), limit)) {
            log.warn("Rate limit exceeded — ip={} path={}", ip, path);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private int resolveLimit(String path, String method) {
        if (path.startsWith("/api/auth/"))                              return AUTH_RPM;
        if ("POST".equals(method) && path.startsWith("/api/media/"))    return UPLOAD_RPM;
        return 0; // no limit on other routes
    }

    private String bucketKey(String path) {
        if (path.startsWith("/api/auth/"))   return "auth";
        if (path.startsWith("/api/media/"))  return "media";
        return "general";
    }

    /**
     * Sliding-window counter. Returns true if the IP has exceeded its allowance.
     * Uses ConcurrentHashMap.compute() for thread-safe, lock-free atomicity.
     */
    private boolean isRateLimited(String key, int limit) {
        long now = System.currentTimeMillis();
        final boolean[] exceeded = {false};

        windowMap.compute(key, (k, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            // Evict timestamps outside the current window
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                exceeded[0] = true;
            } else {
                deque.addLast(now);
            }
            // Return null when the deque is empty to let the map GC the entry
            return deque.isEmpty() ? null : deque;
        });

        return exceeded[0];
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getHostString()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return -2; // runs before AuthenticationFilter (order -1)
    }
}
