package com.mihaiciobotaru.eshop.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDto {
    private String firstName;

    private String lastName;

    @Email(message = "Email should be valid.")
    private String email;

    @NotBlank(message = "Password cannot be blank.")
    private String password;

    @NotBlank(message = "Password cannot be blank.")
    private String matchingPassword;

    @NotBlank(message = "User name cannot be blank.")
    private String username;

    public UserDto() {
    }

    public UserDto(String firstName, String lastName, String email, String password, String matchingPassword, String username) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.matchingPassword = matchingPassword;
        this.username = username;
    }

    public String getMatchingPassword() {
        return matchingPassword;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMatchingPassword(String matchingPassword) {
        this.matchingPassword = matchingPassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
