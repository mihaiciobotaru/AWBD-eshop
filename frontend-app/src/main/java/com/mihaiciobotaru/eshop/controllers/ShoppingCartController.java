package com.mihaiciobotaru.eshop.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.mihaiciobotaru.eshop.exception.ResourceNotFoundException;
import com.mihaiciobotaru.eshop.models.Product;
import com.mihaiciobotaru.eshop.models.ShoppingCart;
import com.mihaiciobotaru.eshop.models.User;
import com.mihaiciobotaru.eshop.service.ProductService;
import com.mihaiciobotaru.eshop.service.ShoppingCartService;
import com.mihaiciobotaru.eshop.service.UserService;
import com.mihaiciobotaru.eshop.config.GlobalControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;


@Controller
@RequestMapping ("/api/shopping-cart")
public class ShoppingCartController {
    private final ShoppingCartService shoppingCartService;
    private final UserService userService;
    private final ProductService productService;
    private final GlobalControllerAdvice globalControllerAdvice;
    private final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);

    public ShoppingCartController(ShoppingCartService shoppingCartService, UserService userService, ProductService productService, GlobalControllerAdvice globalControllerAdvice) {
        this.shoppingCartService = shoppingCartService;
        this.userService = userService;
        this.productService = productService;
        this.globalControllerAdvice = globalControllerAdvice;
    }

    @GetMapping("/view")
    public String viewCart(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ShoppingCart cart = shoppingCartService.getCartByUser_Id(user.getId());
        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", cart.getCartItems());
        model.addAttribute("totalPrice", cart.getTotalPrice());
        return "shopping-cart";
    }

    @PostMapping("/add")
    public String addItemToCart(@RequestParam Long productId, @RequestParam int quantity, RedirectAttributes redirectAttributes) {
        logger.info("Attempting to add item to cart: productId={}, quantity={}", productId, quantity);
        Optional<User> user = globalControllerAdvice.getCurrentUser();

        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to add items to the cart");
            return "redirect:/login";
        }

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        shoppingCartService.addProductToCart(user.get().getId(), product, quantity);

        redirectAttributes.addFlashAttribute("message", "Item added to cart successfully");
        return "redirect:/api/products";
    }

    @PostMapping("/decrease")
    public String decreaseItemQuantity(@RequestParam Long productId, RedirectAttributes redirectAttributes) {
        logger.info("Attempting to decrease item quantity in cart: productId={}", productId);
        Optional<User> user = globalControllerAdvice.getCurrentUser();

        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to modify the cart");
            return "redirect:/login";
        }

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        shoppingCartService.decreaseProductQuantity(user.get().getId(), product.getId());

        redirectAttributes.addFlashAttribute("message", "Item quantity decreased successfully");
        return "redirect:/api/shopping-cart/view";
    }

    @PostMapping("/increase")
    public String increaseItemQuantity(@RequestParam Long productId, RedirectAttributes redirectAttributes) {
        logger.info("Attempting to increase item quantity in cart: productId={}", productId);
        Optional<User> user = globalControllerAdvice.getCurrentUser();

        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to modify the cart");
            return "redirect:/login";
        }

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        shoppingCartService.increaseProductQuantity(user.get().getId(), product.getId());

        redirectAttributes.addFlashAttribute("message", "Item quantity increased successfully");
        return "redirect:/api/shopping-cart/view";
    }

}
