package com.mihaiciobotaru.eshop;

import com.mihaiciobotaru.eshop.exception.ResourceNotFoundException;
import com.mihaiciobotaru.eshop.models.Product;
import com.mihaiciobotaru.eshop.models.ShoppingCart;
import com.mihaiciobotaru.eshop.models.User;
import com.mihaiciobotaru.eshop.service.ProductService;
import com.mihaiciobotaru.eshop.service.ShoppingCartService;
import com.mihaiciobotaru.eshop.service.UserService;
import com.mihaiciobotaru.eshop.controllers.ShoppingCartController;
import com.mihaiciobotaru.eshop.config.GlobalControllerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ShoppingCartControllerTest {

    @Mock
    private ShoppingCartService shoppingCartService;
    @Mock
    private UserService userService;
    @Mock
    private ProductService productService;
    @Mock
    private GlobalControllerAdvice globalControllerAdvice;
    @Mock
    private Model model;
    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private ShoppingCartController shoppingCartController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- viewCart tests ---

    @Test
    void viewCart_authenticatedUser_success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        User user = new User();
        user.setId(1L);
        when(userService.findByUsername("user")).thenReturn(Optional.of(user));
        ShoppingCart cart = mock(ShoppingCart.class);
        when(shoppingCartService.getCartByUser_Id(1L)).thenReturn(cart);
        when(cart.getCartItems()).thenReturn(List.of());
        when(cart.getTotalPrice()).thenReturn(100.0);

        String view = shoppingCartController.viewCart(model);

        assertEquals("shopping-cart", view);
        verify(model).addAttribute(eq("cart"), eq(cart));
        verify(model).addAttribute(eq("cartItems"), any());
        verify(model).addAttribute(eq("totalPrice"), eq(100.0));
    }

    @Test
    void viewCart_unauthenticatedUser_redirectsToLogin() {
        SecurityContextHolder.getContext().setAuthentication(null);

        String view = shoppingCartController.viewCart(model);

        assertEquals("redirect:/login", view);
    }

    @Test
    void viewCart_userNotFound_throwsException() {
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsername("user")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shoppingCartController.viewCart(model));
    }

    @Test
    void addItemToCart_userNotLoggedIn_redirectsToLogin() {
        when(globalControllerAdvice.getCurrentUser()).thenReturn(Optional.empty());

        String view = shoppingCartController.addItemToCart(1L, 2, redirectAttributes);

        assertEquals("redirect:/login", view);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void addItemToCart_productNotFound_throwsException() {
        User user = new User();
        user.setId(1L);
        when(globalControllerAdvice.getCurrentUser()).thenReturn(Optional.of(user));
        when(productService.getProductById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shoppingCartController.addItemToCart(1L, 2, redirectAttributes));
    }

    @Test
    void addItemToCart_success() {
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        when(globalControllerAdvice.getCurrentUser()).thenReturn(Optional.of(user));
        when(productService.getProductById(1L)).thenReturn(Optional.of(product));

        String view = shoppingCartController.addItemToCart(1L, 2, redirectAttributes);

        assertEquals("redirect:/api/products", view);
        verify(shoppingCartService).addProductToCart(1L, product, 2);
        verify(redirectAttributes).addFlashAttribute(eq("message"), anyString());
    }

    @Test
    void decreaseItemQuantity_userNotLoggedIn_redirectsToLogin() {
        when(globalControllerAdvice.getCurrentUser()).thenReturn(Optional.empty());

        String view = shoppingCartController.decreaseItemQuantity(1L, redirectAttributes);

        assertEquals("redirect:/login", view);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void decreaseItemQuantity_productNotFound_throwsException() {
        User user = new User();
        user.setId(1L);
        when(globalControllerAdvice.getCurrentUser()).thenReturn(Optional.of(user));
        when(productService.getProductById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shoppingCartController.decreaseItemQuantity(1L, redirectAttributes));
    }

    @Test
    void decreaseItemQuantity_success() {
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(1L);
        when(globalControllerAdvice.getCurrentUser()).thenReturn(Optional.of(user));
        when(productService.getProductById(1L)).thenReturn(Optional.of(product));

        String view = shoppingCartController.decreaseItemQuantity(1L, redirectAttributes);

        assertEquals("redirect:/api/shopping-cart/view", view);
        verify(shoppingCartService).decreaseProductQuantity(1L, 1L);
        verify(redirectAttributes).addFlashAttribute(eq("message"), anyString());
    }

    @Test
    void increaseItemQuantity_userNotLoggedIn_redirectsToLogin() {
        when(globalControllerAdvice.getCurrentUser()).thenReturn(Optional.empty());

        String view = shoppingCartController.increaseItemQuantity(1L, redirectAttributes);

        assertEquals("redirect:/login", view);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void increaseItemQuantity_productNotFound_throwsException() {
        User user = new User();
        user.setId(1L);
        when(globalControllerAdvice.getCurrentUser()).thenReturn(Optional.of(user));
        when(productService.getProductById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shoppingCartController.increaseItemQuantity(1L, redirectAttributes));
    }

    @Test
    void increaseItemQuantity_success() {
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(1L);
        when(globalControllerAdvice.getCurrentUser()).thenReturn(Optional.of(user));
        when(productService.getProductById(1L)).thenReturn(Optional.of(product));

        String view = shoppingCartController.increaseItemQuantity(1L, redirectAttributes);

        assertEquals("redirect:/api/shopping-cart/view", view);
        verify(shoppingCartService).increaseProductQuantity(1L, 1L);
        verify(redirectAttributes).addFlashAttribute(eq("message"), anyString());
    }
}