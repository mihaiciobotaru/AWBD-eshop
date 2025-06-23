package com.eshop.apigateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered; // Import Ordered
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.util.AntPathMatcher; // Import AntPathMatcher

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom GatewayFilterFactory for centralized role-based authorization.
 * It reads user roles from the 'X-Auth-Roles' header (set by TokenValidationGatewayFilterFactory)
 * and checks if the user has the required roles to access the requested path.
 *
 * Implements Ordered to ensure it runs with a specific precedence.
 */
@Component
public class AuthorizationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthorizationGatewayFilterFactory.Config> implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationGatewayFilterFactory.class);
    private final AntPathMatcher pathMatcher = new AntPathMatcher(); // Initialize AntPathMatcher

    // Define endpoint-role mappings for simple authorization.
    // The keys are Ant-style path patterns, values are the roles required for that pattern.
    // A request is authorized if the user has *at least one* of the required roles for a matching path.
    private static final Map<String, Set<String>> PATH_ROLES_MAP = Map.of(
            "/api/products/admin/**", Set.of("ADMIN"),
            "/api/categories/admin/**", Set.of("ADMIN"),
            "/api/users/admin/**", Set.of("ADMIN"),
            "/auth/me", Set.of("USER", "ADMIN"),
            "/api/carts/me", Set.of("USER", "ADMIN"),
            "/api/carts/**", Set.of("ADMIN") // Broader pattern for other cart operations, usually admin-only
    );

    /**
     * Constructor for the AuthorizationGatewayFilterFactory.
     * Calls the super constructor to register the Config class.
     */
    public AuthorizationGatewayFilterFactory() {
        super(Config.class); // Necessary for AbstractGatewayFilterFactory
    }

    /**
     * Sets the order of this filter in the chain.
     * A lower value means higher precedence (runs earlier).
     * We set it to run very early to ensure its response completion
     * happens before other default filters that might interfere.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5; // Run very early, but allow some Spring internal filters to precede
    }

    /**
     * This is the main method where the filter logic is applied.
     * @param config The configuration for this specific filter instance (currently empty).
     * @return A GatewayFilter instance containing the actual filter logic.
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            String path = request.getURI().getPath();

            // Get user roles from header (set by TokenValidationGatewayFilterFactory)
            String rolesHeader = request.getHeaders().getFirst("X-Auth-Roles");
            Set<String> userRoles = (rolesHeader != null && !rolesHeader.isEmpty()) ?
                                     Arrays.stream(rolesHeader.split(",")).map(String::trim).collect(Collectors.toSet()) :
                                     Collections.emptySet();

            // Find all required roles for the current path using AntPathMatcher
            Set<String> requiredRolesForPath = PATH_ROLES_MAP.entrySet().stream()
                    .filter(entry -> pathMatcher.match(entry.getKey(), path)) // Use AntPathMatcher
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toSet());

            // If no specific roles are configured for this path in PATH_ROLES_MAP,
            // we assume it just needs to be authenticated (which TokenValidationGatewayFilterFactory
            // should have already ensured).
            if (requiredRolesForPath.isEmpty()) {
                logger.debug("Path {} does not have specific role requirements in PATH_ROLES_MAP. Allowing if authenticated.", path);
                return chain.filter(exchange);
            }

            // Check if the user has at least one of the required roles
            boolean authorized = userRoles.stream().anyMatch(requiredRolesForPath::contains);

            if (!authorized) {
                logger.warn("Authorization failed for {}: User roles {} do not meet required roles {}.", path, userRoles, requiredRolesForPath);
                response.setStatusCode(HttpStatus.FORBIDDEN); // 403 Forbidden
                return response.setComplete(); // Complete the response to short-circuit the chain
            }

            logger.debug("Authorization successful for path {}. User roles: {}", path, userRoles);
            return chain.filter(exchange);
        };
    }

    /**
     * Configuration class for the AuthorizationGatewayFilterFactory.
     * Currently, this filter does not require any specific configuration properties
     * to be set in the application.yml/properties, so this class can be empty.
     * However, it's still needed because AbstractGatewayFilterFactory expects it.
     */
    public static class Config {
        // No specific properties needed for this filter, but it must exist.
    }
}
