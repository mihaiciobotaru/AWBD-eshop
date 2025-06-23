// src/main/java/com/eshop/frontend/security/AuthServiceAuthenticationProvider.java
package com.eshop.frontend.security;

import com.eshop.frontend.dto.AuthenticationRequest;
import com.eshop.frontend.dto.AuthenticationResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

public class AuthServiceAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceAuthenticationProvider.class);

    @Value("${microservice.auth-service.url}")
    private String authServiceBaseUrl;

    private final RestTemplate restTemplate;

    public AuthServiceAuthenticationProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        String authServiceLoginUrl = authServiceBaseUrl + "/auth/login";
        AuthenticationRequest authRequest = new AuthenticationRequest(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthenticationRequest> requestEntity = new HttpEntity<>(authRequest, headers);

        // --- DIAGNOSTIC LOGGING: Before Auth Service Call ---
        ServletRequestAttributes preAuthAttr = null;
        HttpSession preAuthSession = null;
        if (RequestContextHolder.currentRequestAttributes() instanceof ServletRequestAttributes) {
            preAuthAttr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            preAuthSession = preAuthAttr.getRequest().getSession(false); // Don't create if not exists
            if (preAuthSession != null) {
                logger.debug("AuthServiceAuthenticationProvider: Before auth-service call, Session ID: {}", preAuthSession.getId());
            } else {
                logger.debug("AuthServiceAuthenticationProvider: Before auth-service call, No existing session.");
            }
        } else {
            logger.debug("AuthServiceAuthenticationProvider: Before auth-service call, RequestContextHolder is not ServletRequestAttributes or null.");
        }
        // --- END DIAGNOSTIC LOGGING ---

        try {
            logger.info("Attempting to authenticate user {} with auth-service at {}", username, authServiceLoginUrl);
            ResponseEntity<AuthenticationResponse> response = restTemplate.postForEntity(
                    authServiceLoginUrl,
                    requestEntity,
                    AuthenticationResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String jwtToken = response.getBody().getJwtToken();

                // --- NEW DIAGNOSTIC: Log the actual JWT value received ---
                if (jwtToken == null) {
                    logger.error("AuthServiceAuthenticationProvider: Received JWT token is NULL from auth-service for user {}.", username);
                } else if (jwtToken.isEmpty()) {
                    logger.error("AuthServiceAuthenticationProvider: Received JWT token is EMPTY from auth-service for user {}.", username);
                } else {
                    logger.info("Authentication successful for user {}. JWT received from auth-service (length: {}).", username, jwtToken.length());
                    // Log first few characters of JWT for confirmation
                    logger.debug("AuthServiceAuthenticationProvider: Received JWT (first 20 chars): {}", jwtToken.substring(0, Math.min(jwtToken.length(), 20)));
                }
                // --- END NEW DIAGNOSTIC ---

                ServletRequestAttributes postAuthAttr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                if (postAuthAttr != null) {
                    HttpSession session = postAuthAttr.getRequest().getSession(true); // Get or create session
                    logger.debug("AuthServiceAuthenticationProvider: After auth success, Session object: {} (ID: {})", session.getClass().getName(), session.getId());

                    if (jwtToken != null && !jwtToken.isEmpty()) {
                        session.setAttribute("jwtToken", jwtToken);
                        logger.debug("AuthServiceAuthenticationProvider: Attempted to store JWT in session.");

                        // --- NEW DIAGNOSTIC: Immediate retrieval check ---
                        Object retrievedToken = session.getAttribute("jwtToken");
                        if (retrievedToken != null && retrievedToken instanceof String && !((String)retrievedToken).isEmpty()) {
                            logger.debug("AuthServiceAuthenticationProvider: Confirmed JWT EXISTS in session immediately after setting. Retrieved token length: {}", ((String)retrievedToken).length());
                        } else {
                            logger.error("AuthServiceAuthenticationProvider: ERROR - JWT NOT FOUND or invalid type in session immediately after setting! Retrieved value: {}", retrievedToken);
                        }
                        // --- END NEW DIAGNOSTIC ---
                    } else {
                        logger.error("AuthServiceAuthenticationProvider: Skipping JWT storage because token is null or empty.");
                    }
                } else {
                    logger.warn("AuthServiceAuthenticationProvider: WARN - RequestContextHolder is null AFTER authentication. Cannot store JWT in session.");
                }

                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                return new UsernamePasswordAuthenticationToken(username, null, authorities);

            } else {
                logger.warn("Authentication failed for user {} with status code {}", username, response.getStatusCode());
                throw new BadCredentialsException("Authentication failed: Invalid credentials or unexpected response.");
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            logger.warn("Authentication failed for user {} due to unauthorized access: {}", username, e.getMessage());
            throw new BadCredentialsException("Invalid username or password.");
        } catch (Exception e) {
            logger.error("Error during authentication call to auth-service for user {}: {}", username, e.getMessage(), e);
            throw new BadCredentialsException("Authentication service error. Please try again later.");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}