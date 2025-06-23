// src/main/java/com/eshop/frontend/controllers/FrontendController.java
package com.eshop.frontend.controllers;

import com.eshop.frontend.dto.UserDto;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class FrontendController {

    private static final Logger logger = LoggerFactory.getLogger(FrontendController.class);

    @Value("${microservice.auth-service.url}")
    private String authServiceBaseUrl;

    @Value("${microservice.product-category-service.url}")
    private String productCategoryServiceUrl;

    @Value("${microservice.shopping-cart-service.url}")
    private String shoppingCartServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public FrontendController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/login")
    public String getLoginPage(Model model) {
        return "login";
    }

    @GetMapping("/register")
    public String getRegisterPage(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("userDto") UserDto userDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {

        if (!userDto.getPassword().equals(userDto.getMatchingPassword())) {
            bindingResult.rejectValue("matchingPassword", "error.userDto", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during registration: {}", bindingResult.getAllErrors());
            return "register";
        }

        String registerUrl = authServiceBaseUrl + "/auth/register";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserDto> requestEntity = new HttpEntity<>(userDto, headers);

        try {
            logger.info("Attempting to register user {} with auth-service at {}", userDto.getEmail(), registerUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(registerUrl, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
                logger.info("User {} registered successfully.", userDto.getEmail());
                return "redirect:/login";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: Unexpected response.");
                logger.warn("Registration failed for user {} with status code: {}", userDto.getEmail(), response.getStatusCode());
                return "redirect:/register";
            }
        } catch (HttpClientErrorException e) {
            String errorMessage = "Registration failed.";
            try {
                Map errorResponse = e.getResponseBodyAs(Map.class);
                if (errorResponse != null && errorResponse.containsKey("message")) {
                    errorMessage = (String) errorResponse.get("message");
                }
            } catch (Exception parseException) {
                logger.error("Failed to parse error response from auth-service: {}", parseException.getMessage());
            }
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            logger.error("HTTP error during registration for {}: {} - {}", userDto.getEmail(), e.getStatusCode(), e.getResponseBodyAsString());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during registration. Please try again.");
            logger.error("Error during user registration for {}: {}", userDto.getEmail(), e.getMessage(), e);
            return "redirect:/register";
        }
    }

    @GetMapping("/home")
    public String getHomePage(Model model, HttpSession session) {
        // --- DIAGNOSTIC LOGGING: When accessing /home ---
        logger.debug("FrontendController: Accessing /home. Current Session ID: {}", session.getId());
        // --- END DIAGNOSTIC LOGGING ---

        String jwtToken = (String) session.getAttribute("jwtToken");

        if (jwtToken != null) {
            logger.debug("FrontendController: JWT token FOUND in session for /home access.");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            try {
                String userInfoUrl = authServiceBaseUrl + "/auth/me";
                logger.info("Calling auth-service /me endpoint for user with token. URL: {}", userInfoUrl);
                ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                    userInfoUrl, HttpMethod.GET, entity, String.class);
                model.addAttribute("userInfo", userInfoResponse.getBody());
                logger.info("Successfully fetched user info from auth-service.");
            } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
                logger.warn("Access denied to /auth/me or token expired for user: {}", e.getMessage());
                model.addAttribute("userInfo", "Could not fetch user info (Unauthorized/Forbidden). Please log in again.");
            } catch (Exception e) {
                logger.error("Error fetching user info from auth-service: {}", e.getMessage(), e);
                model.addAttribute("userInfo", "Error fetching user details.");
            }

            try {
                String products = restTemplate.exchange(productCategoryServiceUrl + "/products/all", HttpMethod.GET, entity, String.class).getBody();
                model.addAttribute("products", products);
            } catch (Exception e) {
                model.addAttribute("products", "Could not load products: " + e.getMessage());
                logger.error("Error calling product-category-service: {}", e.getMessage(), e);
            }

            model.addAttribute("userServiceUrl", authServiceBaseUrl);
            model.addAttribute("productCategoryServiceUrl", productCategoryServiceUrl);
            model.addAttribute("shoppingCartServiceUrl", shoppingCartServiceUrl);

        } else {
            logger.warn("FrontendController: JWT token NOT FOUND in session when accessing /home. Redirecting to login.");
            return "redirect:/login";
        }
        return "home";
    }
}