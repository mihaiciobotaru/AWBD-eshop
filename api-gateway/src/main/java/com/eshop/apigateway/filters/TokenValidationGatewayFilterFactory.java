package com.eshop.apigateway.filters;

import com.eshop.apigateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class TokenValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<TokenValidationGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(TokenValidationGatewayFilterFactory.class);

    private final SecretKey jwtSecretKey;

    /**
     * Constructor for the TokenValidationGatewayFilterFactory.
     * Initializes the JWT secret key from the GatewaySecurityProperties.
     * Calls the super constructor to register the Config class.
     *
     * @param properties The GatewaySecurityProperties bean containing the JWT secret.
     */
    public TokenValidationGatewayFilterFactory(GatewaySecurityProperties properties) {
        // Call the super constructor with the Config class.
        // This is necessary for AbstractGatewayFilterFactory to work correctly.
        super(Config.class);
        this.jwtSecretKey = Keys.hmacShaKeyFor(properties.getJwt().getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * This is the main method where the filter logic is applied.
     * It's called by the Spring Cloud Gateway for each request that matches a route
     * configured to use this filter.
     *
     * @param config The configuration for this specific filter instance (currently empty).
     * @return A GatewayFilter instance containing the actual filter logic.
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            String authHeader = request.getHeaders().getFirst("Authorization");

            // 1. Check for Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Missing or invalid Authorization header for path: {}", request.getURI().getPath());
                response.setStatusCode(HttpStatus.UNAUTHORIZED); // 401 Unauthorized
                return response.setComplete();
            }

            String token = authHeader.substring(7); // Extract JWT (skip "Bearer ")

            try {
                // 2. Parse and validate the JWT token
                io.jsonwebtoken.JwtParser parser = Jwts.parser()
                        .setSigningKey(jwtSecretKey)
                        .build();

                Claims claims = parser.parseClaimsJws(token).getBody();

                // 3. Check for token expiration
                if (claims.getExpiration().before(new Date())) {
                    logger.warn("Expired JWT token for request to {}: Subject {}", request.getURI().getPath(), claims.getSubject());
                    throw new ExpiredJwtException(null, claims, "JWT token is expired.");
                }

                // 4. Add user claims to request headers for downstream microservices
                ServerHttpRequest.Builder builder = request.mutate();
                builder.header("X-Auth-Username", claims.getSubject()); // Subject is typically the username/user ID

                if (claims.containsKey("roles")) {
                    List<String> roles = (List<String>) claims.get("roles"); // Assuming roles are a List<String>
                    builder.header("X-Auth-Roles", String.join(",", roles)); // Comma-separated roles
                } else {
                    // Default to a 'USER' role if no roles claim is present
                    builder.header("X-Auth-Roles", "USER");
                }

                logger.debug("JWT validated. User: {}, Roles: {} for path: {}",
                        claims.getSubject(), claims.containsKey("roles") ? String.join(",", (List<String>) claims.get("roles")) : "USER", request.getURI().getPath());

                // 5. Continue the filter chain with the modified request
                return chain.filter(exchange.mutate().request(builder.build()).build());

            } catch (SignatureException e) {
                logger.error("Invalid JWT signature for request to {}: {}", request.getURI().getPath(), e.getMessage());
                response.setStatusCode(HttpStatus.UNAUTHORIZED); // 401 Unauthorized
                return response.setComplete();
            } catch (ExpiredJwtException e) {
                logger.warn("Expired JWT token for request to {}: {}", request.getURI().getPath(), e.getMessage());
                response.setStatusCode(HttpStatus.UNAUTHORIZED); // 401 Unauthorized
                return response.setComplete();
            } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
                logger.error("Invalid JWT token format or argument for request to {}: {}", request.getURI().getPath(), e.getMessage());
                response.setStatusCode(HttpStatus.BAD_REQUEST); // 400 Bad Request
                return response.setComplete();
            } catch (Exception e) {
                // Catch any other unexpected exceptions during JWT processing
                logger.error("Unexpected error during JWT validation for request to {}: {}", request.getURI().getPath(), e.getMessage(), e);
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
                return response.setComplete();
            }
        };
    }

    /**
     * Configuration class for the TokenValidationGatewayFilterFactory.
     * Currently, this filter doesn't require any specific configuration properties
     * to be set in the application.yml/properties, so this class can be empty.
     * However, it's still needed because AbstractGatewayFilterFactory expects it.
     */
    public static class Config {
        // No specific properties needed for this filter, but it must exist.
    }
}
