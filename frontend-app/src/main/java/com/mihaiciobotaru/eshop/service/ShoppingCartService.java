package com.mihaiciobotaru.eshop.service;

import com.mihaiciobotaru.eshop.models.CartItem;
import com.mihaiciobotaru.eshop.models.ShoppingCart;
import com.mihaiciobotaru.eshop.repository.ShoppingCartRepository;
import com.mihaiciobotaru.eshop.models.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.mihaiciobotaru.eshop.models.Product;

import com.mihaiciobotaru.eshop.repository.UserRepository;

@Service
public class ShoppingCartService {
    private final Logger logger = LoggerFactory.getLogger(ShoppingCartService.class);
    private final ShoppingCartRepository shoppingCartRepository;
    private final UserRepository userRepository;

    public ShoppingCartService(ShoppingCartRepository shoppingCartRepository, UserRepository userRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.userRepository = userRepository;
    }

    public ShoppingCart getCartByUser_Id(Long userId) {
        Optional<ShoppingCart> shoppingCart = shoppingCartRepository.findByUser_Id(userId);
        if (shoppingCart.isPresent()) {
            return shoppingCart.get();
        } else {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            ShoppingCart newCart = new ShoppingCart(user);
            shoppingCartRepository.save(newCart);
            return newCart;
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
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (!cart.isPresent()) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            cart = Optional.of(new ShoppingCart(user));
        }

        cart.get().addCartItem(cartItem);
        shoppingCartRepository.save(cart.get());
    }

    public void addProductToCart(Long userId, Product product, int quantity) {
        logger.info("Adding item {} to shopping cart for user ID: {}", product.getId(), userId);
        Optional<ShoppingCart> cart = shoppingCartRepository.findByUser_Id(userId);
        if (!cart.isPresent()) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            cart = Optional.of(new ShoppingCart(user));
        }

        cart.get().addProductToCart(product, quantity);
        shoppingCartRepository.save(cart.get());
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

    public void createShoppingCartIfNull(Long userId) {
        if (!shoppingCartRepository.findByUser_Id(userId).isPresent()) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            ShoppingCart newCart = new ShoppingCart(user);

            shoppingCartRepository.save(newCart);
            logger.info("Created a new shopping cart for user ID: {}", userId);
        } else {
            logger.info("Shopping cart already exists for user ID: {}", userId);
        }
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

}