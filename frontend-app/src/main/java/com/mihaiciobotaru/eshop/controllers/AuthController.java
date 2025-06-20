package com.mihaiciobotaru.eshop.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mihaiciobotaru.eshop.dto.UserDto;
import com.mihaiciobotaru.eshop.exception.UserAlreadyExistsException;
import com.mihaiciobotaru.eshop.models.Authority;
import com.mihaiciobotaru.eshop.models.User;
import com.mihaiciobotaru.eshop.service.AuthorityService;
import com.mihaiciobotaru.eshop.service.UserService;

import jakarta.validation.Valid;
@Controller
public class AuthController {

    private final UserService userService;
    private final AuthorityService authorityService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService, AuthorityService authorityService) {
        this.userService = userService;
        this.authorityService = authorityService;
    }



    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "register";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "success", required = false) String success,
                        Model model) {

        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
            logger.warn("Login attempt failed due to invalid credentials");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
            logger.info("User logged out successfully");
        }

        if (success != null) {
            model.addAttribute("message", "You have logged in successfully");
            logger.info("User logged in successfully");
            return "redirect:/home";
        }

        return "login";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("userDto") UserDto registrationDto,
                                BindingResult bindingResult, Model model) {

        if (!registrationDto.getPassword().equals(registrationDto.getMatchingPassword())) {
            bindingResult.rejectValue("matchingPassword", "error.userDto", "Passwords do not match");
            logger.warn("Password mismatch for user registration: {}", registrationDto.getEmail());
            return "register";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("userDto", registrationDto);
            bindingResult.reject("error.userDto", "Please correct the errors below: \n " + bindingResult.getAllErrors());
            logger.warn("Validation errors during user registration: {}", bindingResult.getAllErrors());
            return "register";
        }

        try {
            User user = userService.registerNewUser(registrationDto);
            Authority authority = authorityService.createAuthority(user);
            userService.linkToAuthority(user, authority);

        } catch (UserAlreadyExistsException e) {
            bindingResult.rejectValue("email", "error.userDto", "Email already exists");
            logger.warn("User registration failed due to existing email: {}", registrationDto.getEmail());
            return "register";
        } catch (Exception e) {
            bindingResult.reject("error.userDto", "An unexpected error occurred during registration");
            logger.error("Unexpected error during user registration: {}", e.getMessage(), e);
            return "register";
        }
        logger.info("User registered successfully: {}", registrationDto.getEmail());
        return "redirect:/login?registrationSuccess=true";
    }

}
