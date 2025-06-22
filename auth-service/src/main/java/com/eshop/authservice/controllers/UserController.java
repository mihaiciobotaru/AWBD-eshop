package com.eshop.authservice.controllers;
import com.eshop.authservice.models.User;
import com.eshop.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listUsers(Model model, 
                            @RequestParam(defaultValue = "0", required = false) int page,
                            @RequestParam(defaultValue = "10", required = false) int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<User> users = userRepository.findAll(pageable);
        model.addAttribute("users", users);
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);
        return "home";
    }
}