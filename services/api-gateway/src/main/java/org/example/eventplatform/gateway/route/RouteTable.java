package org.example.eventplatform.gateway.route;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * One entry per backend service, by path prefix — matches each service's own
 * controller mappings exactly so there is no ambiguity (e.g. /api/tenant/vendor-profile
 * goes to catalog-service, /api/tenant/events/** goes to event-service, even though
 * both start with /api/tenant/). /api/internal/** is deliberately not routed here:
 * it is service-to-service only and must never be reachable through the public gateway.
 */
@Component
public class RouteTable {

    public record Route(String id, List<String> patterns, String baseUri) {
    }

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<Route> routes;

    public RouteTable(@Value("${services.identity-service.uri}") String identityServiceUri,
                       @Value("${services.catalog-service.uri}") String catalogServiceUri,
                       @Value("${services.event-service.uri}") String eventServiceUri,
                       @Value("${services.customer-service.uri}") String customerServiceUri,
                       @Value("${services.notification-service.uri}") String notificationServiceUri) {
        this.routes = List.of(
                new Route("identity-service",
                        List.of("/api/auth/**", "/api/tenants/**", "/api/users/**"),
                        identityServiceUri),
                new Route("catalog-service",
                        List.of("/api/service-categories/**", "/api/banners/**", "/api/vendor-profiles/**", "/api/tenant/vendor-profile"),
                        catalogServiceUri),
                new Route("event-service",
                        List.of("/api/events/**", "/api/tenant/events/**", "/api/tenant/crew-roles/**",
                                "/api/tenant/show-packages/**", "/api/tenant/withdrawals/**",
                                "/api/customer/**", "/api/public/**", "/api/admin/**"),
                        eventServiceUri),
                new Route("customer-service", List.of("/api/customers/**"), customerServiceUri),
                new Route("notification-service",
                        List.of("/api/fcm/**", "/api/notifications/**"),
                        notificationServiceUri)
        );
    }

    public Route resolve(String path) {
        return routes.stream()
                .filter(route -> route.patterns().stream().anyMatch(p -> pathMatcher.match(p, path)))
                .findFirst()
                .orElse(null);
    }
}
