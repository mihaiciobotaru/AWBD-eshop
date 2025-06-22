package com.eshop.authservice.models;

import com.eshop.authservice.enums.AuthorityEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Table(name = "authorities")
@Entity
public class Authority{

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username cannot be blank.")
    @Size(min = 3, max = 255, message = "Username must be between 3 and 255 characters.")
    private String username;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Authority type cannot be null.")
    private AuthorityEnum authority;

    public Authority() {
    }

    public Authority(String username, AuthorityEnum authority) {
        this.username = username;
        this.authority = authority;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AuthorityEnum getAuthority() {
        return authority;
    }

    public void setAuthority(AuthorityEnum authority) {
        this.authority = authority;
    }

}
