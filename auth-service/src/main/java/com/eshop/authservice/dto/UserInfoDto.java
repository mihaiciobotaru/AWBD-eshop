package com.eshop.authservice.dto;
import com.eshop.authservice.models.Authority;
import lombok.Data;

@Data
public class UserInfoDto {
    private String email;
    private String username;
    private Authority authority;

    public UserInfoDto(String email, String username, Authority authority) {
        this.email = email;
        this.username = username;
        this.authority = authority;
    }
}
