package com.mihaiciobotaru.eshop.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mihaiciobotaru.eshop.models.ShoppingCart;
import com.mihaiciobotaru.eshop.models.User;
import com.mihaiciobotaru.eshop.service.ShoppingCartService;
import com.mihaiciobotaru.eshop.service.UserService;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {
    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerAdvice.class);
    private final ShoppingCartService shoppingCartService;
    private final UserService userService;

    public GlobalControllerAdvice(ShoppingCartService shoppingCartService, UserService userService) {
        this.shoppingCartService = shoppingCartService;
        this.userService = userService;
        logger.info("GlobalControllerAdvice initialized");
    }

    @ModelAttribute("cartItemCount")
    public int getCartItemCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return 0;
        }
        Optional<User> user = userService.findByUsername(authentication.getName());
        if (!user.isPresent()) {
            return 0;
        }
        ShoppingCart cart = shoppingCartService.getCartByUser_Id(user.get().getId());
        if (cart == null) {
            return 0;
        }
        return cart.getTotalQuantity();
    }
    @ModelAttribute("currentUser")
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Optional<User> user = userService.findByUsername(authentication.getName());
        if (!user.isPresent()) {
            logger.warn("User not found for username: {}", authentication.getName());
        }
        return user;
    }

}
