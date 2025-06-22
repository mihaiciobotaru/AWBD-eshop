package com.eshop.shoppingcartservice.service;

import com.eshop.shoppingcartservice.models.CartItem;
import com.eshop.shoppingcartservice.models.ShoppingCart;
import com.eshop.shoppingcartservice.repository.ShoppingCartRepository;
import com.eshop.shoppingcartservice.dto.ProductDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ShoppingCartService {
    private final Logger logger = LoggerFactory.getLogger(ShoppingCartService.class);
    private final ShoppingCartRepository shoppingCartRepository;

    public ShoppingCartService(ShoppingCartRepository shoppingCartRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
    }

    public ShoppingCart getCartByUserId(Long userId) {
        Optional<ShoppingCart> shoppingCart = shoppingCartRepository.findByUser_Id(userId);
        if (shoppingCart.isPresent()) {
            return shoppingCart.get();
        } else {
            return createNewCartForUser(userId);
        }
    }

    public void decreaseProductQuantity(Long userId, Long productId) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (cart.isPresent()) {
            cart.get().getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    int newQuantity = item.getQuantity() - 1;
                    if (newQuantity <= 0) {
                        cart.get().getCartItems().remove(item);
                    } else {
                        item.setQuantity(newQuantity);
                    }
                });
            shoppingCartRepository.save(cart.get());
            logger.info("Decreased quantity of item {} in shopping cart for user ID: {}", productId, userId);
        } else {
            logger.warn("No shopping cart found for user ID: {}", userId);
        }
    }

    public void increaseProductQuantity(Long userId, Long productId) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (cart.isPresent()) {
            cart.get().getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(item.getQuantity() + 1);
                });
            shoppingCartRepository.save(cart.get());
            logger.info("Increased quantity of item {} in shopping cart for user ID: {}", productId, userId);
        } else {
            logger.warn("No shopping cart found for user ID: {}", userId);
        }
    }

    public void addItemToCart(Long userId, CartItem cartItem) {
        logger.info("Adding item {} to shopping cart for user ID: {}", cartItem.getProductId(), userId);
        ShoppingCart cart = shoppingCartRepository.findByUser_Id(userId)
                .orElseGet(() -> createNewCartForUser(userId));

        cart.addCartItem(cartItem);
        shoppingCartRepository.save(cart);
    }

    public void addProductToCart(Long userId, ProductDto product, int quantity) {
        logger.info("Adding item {} to shopping cart for user ID: {}", product, userId);
        ShoppingCart cart = shoppingCartRepository.findByUser_Id(userId)
                .orElseGet(() -> createNewCartForUser(userId));

        cart.addProductToCart(product, quantity);
        shoppingCartRepository.save(cart);
    }

    public void removeItemFromCart(Long userId, Long productId) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (cart.isPresent()) {
            cart.get().getCartItems().removeIf(item -> item.getProductId().equals(productId));
            shoppingCartRepository.save(cart.get());
            logger.info("Removed item {} from shopping cart for user ID: {}", productId, userId);
        } else {
            logger.warn("No shopping cart found for user ID: {}", userId);
        }
    }

    public void clearCart(Long userId) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (cart.isPresent()) {
            cart.get().getCartItems().clear();
            shoppingCartRepository.save(cart.get());
            logger.info("Cleared shopping cart for user ID: {}", userId);
        } else {
            logger.warn("No shopping cart found to clear for user ID: {}", userId);
        }
    }

    public void updateItemQuantity(Long userId, Long productId, int quantity) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (cart.isPresent()) {
            cart.get().getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));
            shoppingCartRepository.save(cart.get());
            logger.info("Updated quantity of item {} in shopping cart for user ID: {}", productId, userId);
        } else {
            logger.warn("No shopping cart found for user ID: {}", userId);
        }
    }

    public List<CartItem> getCartItems(Long userId) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (cart.isPresent()) {
            logger.info("Retrieved {} items from shopping cart for user ID: {}", cart.get().getCartItems().size(), userId);
            return cart.get().getCartItems();
        } else {
            logger.warn("No shopping cart found for user ID: {}", userId);
            return Collections.emptyList();
        }
    }

    public double getTotalPrice(Long userId) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (cart.isPresent()) {
            double totalPrice = cart.get().getCartItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
            logger.info("Total price for shopping cart of user ID {}: {}", userId, totalPrice);
            return totalPrice;
        } else {
            logger.warn("No shopping cart found for user ID: {}", userId);
            return 0.0;
        }
    }

    public int getTotalQuantity(Long userId) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (cart.isPresent()) {
            int totalQuantity = cart.get().getCartItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
            logger.info("Total quantity for shopping cart of user ID {}: {}", userId, totalQuantity);
            return totalQuantity;
        } else {
            logger.warn("No shopping cart found for user ID: {}", userId);
            return 0;
        }
    }

    public ShoppingCart createShoppingCartIfNull(Long userId) {
        Optional<ShoppingCart> existingCart = shoppingCartRepository.findByUser_Id(userId);
        if (existingCart.isPresent()) {
            logger.info("Found existing shopping cart for user ID: {}", userId);
            return existingCart.get();
        }
        return createNewCartForUser(userId);
    }

    public void saveShoppingCart(ShoppingCart shoppingCart) {
        shoppingCartRepository.save(shoppingCart);
        logger.info("Saved shopping cart for user ID: {}", shoppingCart.getUserId());
    }

    public void deleteShoppingCart(Long userId) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (cart.isPresent()) {
            shoppingCartRepository.delete(cart.get());
            logger.info("Deleted shopping cart for user ID: {}", userId);
        } else {
            logger.warn("No shopping cart found to delete for user ID: {}", userId);
        }
    }

    public ShoppingCart createNewCartForUser(Long userId) {
        ShoppingCart newCart = new ShoppingCart();
        newCart.setCartItems(Collections.emptyList());
        shoppingCartRepository.save(newCart);
        logger.info("Created new shopping cart for user ID: {}", userId);
        return newCart;
    }

}