package com.eshop.shoppingcartservice.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import com.eshop.shoppingcartservice.converter.CartItemConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;

import java.util.ArrayList;
import java.util.List;

import com.eshop.shoppingcartservice.dto.ProductDto;
import com.eshop.shoppingcartservice.models.CartItem;

@Entity
@Table(name = "shopping_cart")
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @NotNull(message = "User cannot be null.")
    private Long userId;

    @Column(name = "cart_items", columnDefinition = "TEXT")
    @Convert(converter = CartItemConverter.class)
    private List<CartItem> cartItems = new ArrayList<>();

    public ShoppingCart() {
    }

    public ShoppingCart(Long userId) {
        this.userId = userId;
        this.cartItems = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void addProductToCart(ProductDto product, int quantity) {
        for (CartItem item : this.cartItems) {
            if (item.getProductId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        CartItem cartItem = new CartItem(
                product.getId(),
                product.getName(),
                quantity,
                product.getPrice()
        );

        if (this.cartItems == null) {
            this.cartItems = new ArrayList<>();
        }
        this.cartItems.add(cartItem);
    }

    public void addProductToCart(ProductDto product) {
        addProductToCart(product, 1);
    }

    public void addCartItem(CartItem cartItem) {
        for (CartItem item : this.cartItems) {
                if (item.getProductId().equals(cartItem.getProductId())) {
                    item.setQuantity(item.getQuantity() + cartItem.getQuantity());
                    return;
                }
            }
            if (this.cartItems == null) {
                this.cartItems = new ArrayList<>();
            }

        this.cartItems.add(cartItem);
    }

    public void removeProductFromCart(ProductDto product) {
        this.cartItems.removeIf(item -> item.getProductId().equals(product.getId()));
    }

    public List<CartItem> getCartItems() {
        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public double getTotalPrice() {
        return cartItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    public int getTotalQuantity() {
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public void clearCart() {
        this.cartItems.clear();
    }

    public void updateCartItemQuantity(Long productId, int quantity) {
        for (CartItem item : this.cartItems) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(quantity);
                return;
            }
        }
        throw new IllegalArgumentException("Product not found in cart.");
    }

    @Override
    public String toString() {
        return "ShoppingCart{" +
                "id=" + id +
                ", userId=" + userId +
                ", cartItems=" + cartItems +
                '}';
    }
}
