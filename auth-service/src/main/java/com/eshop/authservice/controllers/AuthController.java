package com.eshop.authservice.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eshop.authservice.dto.AuthenticationRequest;
import com.eshop.authservice.dto.AuthenticationResponse;
import com.eshop.authservice.dto.UserDto;
import com.eshop.authservice.dto.UserInfoDto;
import com.eshop.authservice.exception.UserAlreadyExistsException;
import com.eshop.authservice.models.Authority;
import com.eshop.authservice.models.User;
import com.eshop.authservice.security.JwtService;
import com.eshop.authservice.service.AuthorityService;
import com.eshop.authservice.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping ("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthorityService authorityService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService, AuthorityService authorityService,
                          PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.userService = userService;
        this.authorityService = authorityService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDto registrationDto,
                                          BindingResult bindingResult) {

        if (!registrationDto.getPassword().equals(registrationDto.getMatchingPassword())) {
            bindingResult.rejectValue("matchingPassword", "error.userDto", "Passwords do not match");
            logger.warn("Password mismatch for user registration: {}", registrationDto.getEmail());
        }

        if (bindingResult.hasErrors()) {
            Map<String, List<String>> errors = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                    ));
            logger.warn("Validation errors during user registration: {}", errors);
            return new ResponseEntity<>(createErrorResponse("Validation failed. Please check the 'errors' field for details.", errors), HttpStatus.BAD_REQUEST);
        }

        try {
            User user = userService.registerNewUser(registrationDto);
            Authority authority = authorityService.createAuthority(user);
            userService.linkToAuthority(user, authority);

        } catch (UserAlreadyExistsException e) {

            logger.warn("User registration failed due to existing email/username: {}", registrationDto.getEmail());
            return new ResponseEntity<>(createErrorResponse(e.getMessage(), null), HttpStatus.CONFLICT); // 409 Conflict
        } catch (Exception e) {

            logger.error("Unexpected error during user registration for {}: {}", registrationDto.getEmail(), e.getMessage(), e);
            return new ResponseEntity<>(createErrorResponse("An unexpected error occurred during registration.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        logger.info("User registered successfully: {}", registrationDto.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String jwtToken = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(new AuthenticationResponse(jwtToken));

        } catch (AuthenticationException e) {

            logger.warn("Authentication failed for user {}: {}", request.getUsername(), e.getMessage());
            return new ResponseEntity<>(createErrorResponse("Invalid username or password.", null), HttpStatus.UNAUTHORIZED); // 401 Unauthorized
        } catch (Exception e) {

            logger.error("Unexpected error during login for user {}: {}", request.getUsername(), e.getMessage(), e);
            return new ResponseEntity<>(createErrorResponse("An unexpected error occurred during login.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUserInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Attempt to access /me endpoint without authenticated user.");
                return new ResponseEntity<>(createErrorResponse("User not authenticated.", null), HttpStatus.UNAUTHORIZED);
            }

            String username = authentication.getName();

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.error("Authenticated user '{}' not found in database.", username);
                        return new RuntimeException("Authenticated user not found.");
                    });
           UserInfoDto userDto = new UserInfoDto(user.getEmail(), user.getUsername(), user.getAuthority());


            logger.info("Successfully fetched info for authenticated user: {}", username);
            return ResponseEntity.ok(userDto);

        } catch (Exception e) {
            logger.error("Error fetching authenticated user info: {}", e.getMessage(), e);
            return new ResponseEntity<>(createErrorResponse("Error fetching user info.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> createErrorResponse(String message, Map<String, List<String>> errors) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        if (errors != null && !errors.isEmpty()) {
            errorResponse.put("errors", errors);
        }
        return errorResponse;
    }
}