package com.eshop.shoppingcartservice.controllers;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.eshop.shoppingcartservice.service.ShoppingCartService;
import com.eshop.shoppingcartservice.models.ShoppingCart;
import com.eshop.shoppingcartservice.dto.ProductDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping ("/shopping-cart")
public class ShoppingCartController {
    private final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);
    private final ShoppingCartService shoppingCartService;

    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @GetMapping("/get")
    public ResponseEntity<?> getCartByUserId(@RequestParam Long userId) {
        if (userId == null || userId <= 0) {
            logger.error("Invalid user ID: {}", userId);
            return new ResponseEntity<>(
                Map.of("error", "User ID cannot be null or negative"), HttpStatus.BAD_REQUEST);
        }

        ShoppingCart cart = shoppingCartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItemToCart(@RequestParam Long userId,
                                            @Valid @RequestBody ProductDto productDto, 
                                            @RequestParam int quantity) {
        logger.info("Attempting to add item to cart: userId={}, productDto={}, quantity={}", userId, productDto, quantity);
        if (userId == null || userId <= 0) {
            logger.error("Invalid user ID: {}", userId);
            return new ResponseEntity<>(
                Map.of("error", "User ID cannot be null or negative"), HttpStatus.BAD_REQUEST);
        }
        try {
            shoppingCartService.addProductToCart(userId, productDto, quantity);
        } catch (Exception e) {
            logger.error("Error adding item to cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add item to cart");
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/decrease")
    public ResponseEntity<?> decreaseItemQuantity(@RequestParam Long userId,
                                                  @RequestParam Long productId) {
        logger.info("Attempting to decrease item quantity in cart: productId={}", productId);

        if (userId == null || userId <= 0) {
            logger.error("Invalid user ID: {}", userId);
            return new ResponseEntity<>(
                Map.of("error", "User ID cannot be null or negative"), HttpStatus.BAD_REQUEST);
        }

        shoppingCartService.decreaseProductQuantity(userId, productId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/increase")
    public ResponseEntity<?> increaseItemQuantity(@RequestParam Long userId,
                                                  @RequestParam Long productId) {
        logger.info("Attempting to increase item quantity in cart: productId={}", productId);

        if (userId == null || userId <= 0) {
            logger.error("Invalid user ID: {}", userId);
            return new ResponseEntity<>(
                Map.of("error", "User ID cannot be null or negative"), HttpStatus.BAD_REQUEST);
        }

        shoppingCartService.increaseProductQuantity(userId, productId);

        return ResponseEntity.ok().build();
    }

}
