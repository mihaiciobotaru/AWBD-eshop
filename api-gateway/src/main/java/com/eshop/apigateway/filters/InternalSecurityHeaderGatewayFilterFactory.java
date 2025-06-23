package com.eshop.apigateway.filters; // Assuming filters package based on TokenValidationGF

import com.eshop.apigateway.config.GatewaySecurityProperties; // Import properties
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * Custom GatewayFilterFactory for adding an internal security header to requests
 * before forwarding them to downstream microservices.
 * This filter gets its secret key directly from GatewaySecurityProperties.
 */
@Component
public class InternalSecurityHeaderGatewayFilterFactory extends AbstractGatewayFilterFactory<InternalSecurityHeaderGatewayFilterFactory.Config> {

    private final String internalGatewaySecretKey;

    /**
     * Constructor for the InternalSecurityHeaderGatewayFilterFactory.
     * Injects GatewaySecurityProperties to get the internal gateway secret key.
     * Calls the super constructor to register the Config class (which can be empty).
     * @param properties The GatewaySecurityProperties bean containing the internal gateway secret.
     */
    public InternalSecurityHeaderGatewayFilterFactory(GatewaySecurityProperties properties) {
        super(Config.class); // Necessary for AbstractGatewayFilterFactory
        this.internalGatewaySecretKey = properties.getInternalGateway().getSecretKey();
    }

    /**
     * Applies the filter logic to add the X-Internal-Gateway-Token header.
     * @param config The configuration for this specific filter instance (empty in this case).
     * @return A GatewayFilter instance containing the actual filter logic.
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Add the X-Internal-Gateway-Token header using the injected secret key.
            // This header is expected by downstream services for internal communication validation.
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Internal-Gateway-Token", internalGatewaySecretKey)
                    .build();

            // Continue the filter chain with the modified request
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    /**
     * Configuration class for the InternalSecurityHeaderGatewayFilterFactory.
     * This filter does not require any specific configuration properties to be set
     * via YAML/properties, so this class remains empty.
     * It is required because this factory extends AbstractGatewayFilterFactory<Config>.
     */
    public static class Config {
        // No specific properties needed for this filter via YAML config.
    }
}
