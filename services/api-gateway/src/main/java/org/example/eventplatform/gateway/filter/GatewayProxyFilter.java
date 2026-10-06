package org.example.eventplatform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.gateway.route.RouteTable;
import org.example.eventplatform.shared.security.JwtTokenProvider;
import org.springframework.core.Ordered;
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The whole gateway in one filter: resolve which backend a path belongs to,
 * enforce a centralized JWT check (on top of, not instead of, each service's
 * own — defense in depth, and every service stays independently testable
 * without the gateway running), then transparently proxy the request.
 * Public routes here mirror exactly what each service's own SecurityConfig
 * already permits.
 */
@Component
@Slf4j
public class GatewayProxyFilter implements WebFilter, Ordered {

    private record PublicRoute(HttpMethod method, String pattern) {
    }

    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(HttpMethod.POST, "/api/auth/login"),
            new PublicRoute(HttpMethod.POST, "/api/auth/refresh"),
            new PublicRoute(HttpMethod.GET, "/api/auth/tenant-lookup"),
            new PublicRoute(HttpMethod.POST, "/api/auth/customer/register"),
            new PublicRoute(HttpMethod.GET, "/api/public/**"),
            new PublicRoute(HttpMethod.POST, "/api/tenants/register"),
            new PublicRoute(HttpMethod.POST, "/api/events"),
            new PublicRoute(HttpMethod.GET, "/api/posts/public"),
            new PublicRoute(HttpMethod.GET, "/api/posts/public/**"),
            new PublicRoute(HttpMethod.GET, "/api/files/local/**"),
            new PublicRoute(HttpMethod.GET, "/api/service-categories"),
            new PublicRoute(HttpMethod.GET, "/api/vendor-profiles"),
            new PublicRoute(HttpMethod.GET, "/api/vendor-profiles/**")
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtTokenProvider jwtTokenProvider;
    private final RouteTable routeTable;
    private final WebClient webClient;

    private final RateLimiter rateLimiter = new RateLimiter();

    /** Đường công khai dễ bị lạm dụng và mức tối đa mỗi IP trong một khoảng thời gian. */
    private record Limit(HttpMethod method, String path, int max, long windowMillis) {
    }

    private static final List<Limit> LIMITS = List.of(
            new Limit(HttpMethod.POST, "/api/auth/login", 20, 60_000),
            new Limit(HttpMethod.POST, "/api/auth/customer/register", 10, 3_600_000),
            new Limit(HttpMethod.POST, "/api/tenants/register", 5, 3_600_000),
            new Limit(HttpMethod.POST, "/api/auth/delete-account", 10, 3_600_000)
    );

    public GatewayProxyFilter(JwtTokenProvider jwtTokenProvider, RouteTable routeTable, WebClient.Builder webClientBuilder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.routeTable = routeTable;
        // Pool kết nối có hạn dùng để không giữ kết nối chết sau khi một service khởi động lại (lúc deploy), cùng
        // timeout: không có thì một service treo sẽ giữ request của khách mãi và dồn tải lên gateway.
        ConnectionProvider pool = ConnectionProvider.builder("gateway-upstream")
                .maxConnections(200)
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .evictInBackground(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(pool)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
                .responseTimeout(Duration.ofSeconds(30));
        this.webClient = webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

    /**
     * IP khách theo header nginx gắn thêm; lấy phần tử cuối vì phần đầu do chính client khai nên giả mạo được.
     * Không có header (không biết là ai) thì không giới hạn, để khỏi chặn nhầm cả người dùng thật dưới một IP chung.
     */
    private String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        String real = request.getHeaders().getFirst("X-Real-IP");
        return real == null || real.isBlank() ? null : real.trim();
    }

    private Mono<Void> rejectIfRateLimited(ServerWebExchange exchange, ServerHttpRequest request, String path) {
        for (Limit limit : LIMITS) {
            if (limit.method() == request.getMethod() && limit.path().equals(path)) {
                String ip = clientIp(request);
                if (ip != null && !rateLimiter.allow(ip + "|" + path, limit.max(), limit.windowMillis())) {
                    exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(limit.windowMillis() / 1000));
                    return respond(exchange, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                            "Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút");
                }
            }
        }
        return null;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        addCorsHeaders(exchange);
        if (request.getMethod() == HttpMethod.OPTIONS) {
            exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
            return exchange.getResponse().setComplete();
        }

        Mono<Void> limited = rejectIfRateLimited(exchange, request, path);
        if (limited != null) {
            return limited;
        }

        RouteTable.Route route = routeTable.resolve(path);
        if (route == null) {
            return respond(exchange, HttpStatus.NOT_FOUND, "NOT_FOUND", "Không có route cho " + path);
        }

        if (!isPublic(request)) {
            String token = extractToken(request);
            if (token == null || !jwtTokenProvider.validateToken(token) || jwtTokenProvider.isRefreshToken(token)) {
                return respond(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Thiếu hoặc sai access token");
            }
        }

        return proxy(exchange, route);
    }

    // Wide open by design: auth here is a Bearer header the client attaches itself
    // (never a cookie), so a wildcard origin carries none of the CSRF risk it would
    // for cookie-based auth. Needed for the Flutter web build and the future web
    // admin (Phase 8) to call the gateway from a different origin during dev.
    private void addCorsHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        headers.add("Access-Control-Allow-Headers", "*");
        headers.add("Access-Control-Max-Age", "3600");
    }

    private Mono<Void> proxy(ServerWebExchange exchange, RouteTable.Route route) {
        ServerHttpRequest request = exchange.getRequest();
        String query = request.getURI().getRawQuery();
        // Phải dựng URI sẵn thay vì đưa chuỗi cho WebClient: chuỗi bị coi là uri template
        // và mã hoá lại lần nữa, làm hỏng query tiếng Việt (%C3%A0 -> %25C3%25A0).
        URI targetUri = URI.create(route.baseUri() + request.getPath().value() + (query != null ? "?" + query : ""));

        return webClient.method(request.getMethod())
                .uri(targetUri)
                .headers(headers -> {
                    headers.addAll(request.getHeaders());
                    headers.remove(HttpHeaders.HOST);
                    headers.remove(HttpHeaders.CONTENT_LENGTH);
                })
                .body(BodyInserters.fromDataBuffers(request.getBody()))
                .exchangeToMono(clientResponse -> {
                    exchange.getResponse().setStatusCode(clientResponse.statusCode());
                    HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
                    responseHeaders.addAll(clientResponse.headers().asHttpHeaders());
                    responseHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
                    return exchange.getResponse().writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                })
                .onErrorResume(ex -> {
                    log.error("Proxy error forwarding {} to {}", request.getPath(), targetUri, ex);
                    return respond(exchange, HttpStatus.BAD_GATEWAY, "BAD_GATEWAY", "Không gọi được service phía sau");
                });
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getPath().value();
        return PUBLIC_ROUTES.stream().anyMatch(r ->
                r.method().equals(request.getMethod()) && pathMatcher.match(r.pattern(), path));
    }

    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        var cookie = request.getCookies().getFirst("access_token");
        return cookie != null ? cookie.getValue() : null;
    }

    // Matches shared-common's ApiResponse shape by hand — this filter runs before any
    // controller/Jackson machinery, so there is no ApiResponse bean to reuse here.
    private Mono<Void> respond(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = "{\"success\":false,\"code\":\"%s\",\"message\":\"%s\",\"data\":null}"
                .formatted(code, message);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
