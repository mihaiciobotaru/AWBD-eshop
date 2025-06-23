package com.eshop.apigateway;

import com.eshop.apigateway.config.GatewaySecurityProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.gateway.properties.remove-transfer-encoding=false",
        "eureka.client.enabled=false",
        // ********************************************************************
        // THESE KEYS MUST BE BASE64 ENCODED
        // For "springCloudGatewayJWTSecretKey123456", use Base64: c3ByaW5nQ2xvdWRHYXRld2F5SldUU2VjcmV0S2V5MTIzNDU2
        "spring.cloud.gateway.jwt.secret-key=c3ByaW5nQ2xvdWRHYXRld2F5SldUU2VjcmV0S2V5MTIzNDU2",
        // For "internalGatewaySecretKey4567890123", use Base64: aW50ZXJuYWxHYXRld2F5U2VjcmV0S2V5NDU2Nzg5MDEyMw==
        "spring.cloud.gateway.internal-gateway.secret-key=aW50ZXJuYWxHYXRld2F5U2VjcmV0S2V5NDU2Nzg5MDEyMw==",
        // ********************************************************************

        "spring.cloud.gateway.routes[0].id=test-auth-service-login",
        "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/login",
        "spring.cloud.gateway.routes[0].filters[0]=InternalSecurityHeader",

        "spring.cloud.gateway.routes[1].id=test-auth-service-me",
        "spring.cloud.gateway.routes[1].uri=http://localhost:8081",
        "spring.cloud.gateway.routes[1].predicates[0]=Path=/auth/me",
        "spring.cloud.gateway.routes[1].filters[0]=TokenValidation",
        "spring.cloud.gateway.routes[1].filters[1]=Authorization",
        "spring.cloud.gateway.routes[1].filters[2]=InternalSecurityHeader",

        "spring.cloud.gateway.routes[2].id=test-cart-service-me",
        "spring.cloud.gateway.routes[2].uri=http://localhost:8081",
        "spring.cloud.gateway.routes[2].predicates[0]=Path=/api/carts/me",
        "spring.cloud.gateway.routes[2].filters[0]=TokenValidation",
        "spring.cloud.gateway.routes[2].filters[1]=Authorization",
        "spring.cloud.gateway.routes[2].filters[2]=InternalSecurityHeader",

        "spring.cloud.gateway.routes[3].id=test-cart-service-userid",
        "spring.cloud.gateway.routes[3].uri=http://localhost:8081",
        "spring.cloud.gateway.routes[3].predicates[0]=Path=/api/carts/{userId}",
        "spring.cloud.gateway.routes[3].filters[0]=TokenValidation",
        "spring.cloud.gateway.routes[3].filters[1]=Authorization",
        "spring.cloud.gateway.routes[3].filters[2]=InternalSecurityHeader"
})
public class ApiGatewayServiceApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private GatewaySecurityProperties gatewaySecurityProperties;

    private String jwtSecretKey;
    private String internalGatewaySecretKey;

    // Declare WireMock server
    private static WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 8081; // Using a common port for mock server

    /**
     * Set up WireMock server once before all tests.
     */
    @BeforeAll
    static void setUpWireMock() {
        wireMockServer = new WireMockServer(options().port(WIREMOCK_PORT));
        wireMockServer.start();
        WireMock.configureFor("localhost", WIREMOCK_PORT);
        System.out.println("WireMock server started on port " + WIREMOCK_PORT);
    }

    /**
     * Shut down WireMock server once after all tests.
     */
    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            System.out.println("WireMock server stopped.");
        }
    }

    /**
     * Helper method to generate a valid JWT token for testing.
     */
    private String generateToken(String subject, List<String> roles, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + 3600000; // Token valid for 1 hour
        Date now = new Date(nowMillis);
        Date expiration = new Date(expMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Helper method to generate an expired JWT token for testing.
     */
    private String generateExpiredToken(String subject, List<String> roles, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis - 1000; // Token expired 1 second ago
        Date now = new Date(nowMillis);
        Date expiration = new Date(expMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @BeforeEach
    void setUp() {
        // Clear any previous WireMock stubs before each test
        WireMock.reset();

        this.jwtSecretKey = gatewaySecurityProperties.getJwt().getSecretKey();
        this.internalGatewaySecretKey = gatewaySecurityProperties.getInternalGateway().getSecretKey();

        this.webClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    // --- Authentication (TokenValidationGatewayFilterFactory) Tests ---

    @Test
    @DisplayName("Should allow access with valid JWT token for a secured route and forward to mock service")
    void shouldAllowAccessWithValidJwtToken() {
        String validToken = generateToken("testUser", List.of("USER"), jwtSecretKey);

        // Configure WireMock to respond for the /auth/me path
        stubFor(get(urlEqualTo("/auth/me"))
                .withHeader("X-Auth-Username", equalTo("testUser"))
                .withHeader("X-Auth-Roles", equalTo("USER"))
                .withHeader("X-Internal-Gateway-Token", equalTo(internalGatewaySecretKey))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Auth Service Me Response")));

        webClient.get().uri("/auth/me")
                .header("Authorization", "Bearer " + validToken)
                .exchange()
                .expectStatus().isOk() // Now expect 200 OK from WireMock
                .expectBody(String.class).isEqualTo("Auth Service Me Response");
    }

    @Test
    @DisplayName("Should deny access to secured route without JWT token (401)")
    void shouldDenyAccessWithoutJwtToken() {
        webClient.get().uri("/auth/me")
                .exchange()
                .expectStatus().isUnauthorized(); // Expect 401 from TokenValidationGatewayFilterFactory
    }

    @Test
    @DisplayName("Should deny access to secured route with invalid JWT token signature (401)")
    void shouldDenyAccessWithInvalidJwtSignature() {
        String invalidToken = generateToken("testUser", List.of("USER"), "wrongSecretKey1234567890abcdefghijklmnopqrstuv");

        webClient.get().uri("/auth/me")
                .header("Authorization", "Bearer " + invalidToken)
                .exchange()
                .expectStatus().isUnauthorized(); // Expect 401 from TokenValidationGatewayFilterFactory
    }

    @Test
    @DisplayName("Should deny access to secured route with expired JWT token (401)")
    void shouldDenyAccessWithExpiredJwtToken() {
        String expiredToken = generateExpiredToken("testUser", List.of("USER"), jwtSecretKey);

        webClient.get().uri("/auth/me")
                .header("Authorization", "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized(); // Expect 401 from TokenValidationGatewayFilterFactory
    }

    @Test
    @DisplayName("Should allow public login endpoint without token validation and forward to mock service")
    void shouldAllowPublicLoginWithoutToken() {
        // Configure WireMock for /auth/login
        stubFor(post(urlEqualTo("/auth/login"))
                .withHeader("X-Internal-Gateway-Token", equalTo(internalGatewaySecretKey))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Login success mock")));

        webClient.post().uri("/auth/login")
                .exchange()
                .expectStatus().isOk() // Now expect 200 OK from WireMock
                .expectBody(String.class).isEqualTo("Login success mock");
    }

    // --- Authorization (AuthorizationGatewayFilterFactory) Tests ---

    @Test
    @DisplayName("Should allow access to /auth/me for USER role and forward to mock service")
    void shouldAllowAuthMeForUserRole() {
        String userToken = generateToken("user1", List.of("USER"), jwtSecretKey);

        stubFor(get(urlEqualTo("/auth/me"))
                .withHeader("X-Auth-Username", equalTo("user1"))
                .withHeader("X-Auth-Roles", equalTo("USER"))
                .withHeader("X-Internal-Gateway-Token", equalTo(internalGatewaySecretKey))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Auth Me for User")));

        webClient.get().uri("/auth/me")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Auth Me for User");
    }

    @Test
    @DisplayName("Should allow access to /api/carts/me for USER role and forward to mock service")
    void shouldAllowCartMeForUserRole() {
        String userToken = generateToken("user1", List.of("USER"), jwtSecretKey);

        stubFor(get(urlEqualTo("/api/carts/me"))
                .withHeader("X-Auth-Username", equalTo("user1"))
                .withHeader("X-Auth-Roles", equalTo("USER"))
                .withHeader("X-Internal-Gateway-Token", equalTo(internalGatewaySecretKey))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Cart Me for User")));

        webClient.get().uri("/api/carts/me")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Cart Me for User");
    }

    @Test
    @DisplayName("Should allow access to /api/carts/{userId} for ADMIN role and forward to mock service")
    void shouldAllowCartUserIdForAdminRole() {
        String adminToken = generateToken("adminUser", List.of("ADMIN"), jwtSecretKey); // Token with ADMIN role

        // Configure WireMock for /api/carts/{userId} when accessed by ADMIN
        stubFor(get(urlPathMatching("/api/carts/.*")) // Use urlPathMatching for path variables
                .withHeader("X-Auth-Username", equalTo("adminUser"))
                .withHeader("X-Auth-Roles", equalTo("ADMIN"))
                .withHeader("X-Internal-Gateway-Token", equalTo(internalGatewaySecretKey))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Cart for any User (Admin access)")));

        webClient.get().uri("/api/carts/specificUser123")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Cart for any User (Admin access)");
    }
}
