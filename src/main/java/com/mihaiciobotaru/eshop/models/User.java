package com.mihaiciobotaru.eshop.models;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.NotNull;


@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username cannot be blank.")
    @Size(min = 3, max = 255, message = "Username must be between 3 and 255 characters.")
    private String username;

    @NotBlank(message = "Password cannot be blank.")
    @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters.")
    private String password;

    @Size(min = 5, max = 255, message = "Email must be between 5 and 255 characters.")
    private String email;

    @Size(min = 2, max = 255, message = "First name must be between 2 and 255 characters.")
    private String firstName;

    @Size(min = 2, max = 255, message = "Last name must be between 2 and 255 characters.")
    private String lastName;

    @NotNull
    private Boolean enabled = true;

    @OneToOne
    @JoinColumn(name = "authority_id", nullable = true)
    private Authority authority;

    @OneToOne
    @JoinColumn(name = "shopping_cart_id", nullable = true)
    private ShoppingCart shoppingCart;

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.shoppingCart = new ShoppingCart(this);
    }

    public void setShoppingCart(ShoppingCart shoppingCart) {
        this.shoppingCart = shoppingCart;
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCart;
    }

    public void createShoppingCartIfNull() {
        if (this.shoppingCart == null) {
            this.shoppingCart = new ShoppingCart(this);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Authority getAuthority() {
        return authority;
    }

    public void setAuthority(Authority authority) {
        this.authority = authority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


}
