package com.eshop.authservice.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.eshop.authservice.dto.UserDto;
import com.eshop.authservice.exception.UserAlreadyExistsException;
import com.eshop.authservice.models.Authority;
import com.eshop.authservice.models.User;
import com.eshop.authservice.service.AuthorityService;
import com.eshop.authservice.service.UserService;

import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping ("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthorityService authorityService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService, AuthorityService authorityService) {
        this.userService = userService;
        this.authorityService = authorityService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDto registrationDto,
                                BindingResult bindingResult) {

        if (!registrationDto.getPassword().equals(registrationDto.getMatchingPassword())) {
            bindingResult.rejectValue("matchingPassword", "error.userDto", "Passwords do not match");
            logger.warn("Password mismatch for user registration: {}", registrationDto.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during user registration: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body(Map.of("error", "Validation errors occurred"));
        }

        try {
            User user = userService.registerNewUser(registrationDto);
            Authority authority = authorityService.createAuthority(user);
            userService.linkToAuthority(user, authority);

        } catch (UserAlreadyExistsException e) {
            bindingResult.rejectValue("email", "error.userDto", "Email already exists");
            logger.warn("User registration failed due to existing email: {}", registrationDto.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        } catch (Exception e) {
            bindingResult.reject("error.userDto", "An unexpected error occurred during registration");
            logger.error("Unexpected error during user registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred"));
        }
        logger.info("User registered successfully: {}", registrationDto.getEmail());
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @GetMapping("/get-user-info")
    public ResponseEntity<?> getUserInfo(@RequestParam String email) {
        try {
            User user = userService.findByUsername(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error fetching user info for email {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error fetching user info"));
        }
    }

}
