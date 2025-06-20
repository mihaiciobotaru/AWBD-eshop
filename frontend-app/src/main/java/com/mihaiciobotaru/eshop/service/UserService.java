package com.mihaiciobotaru.eshop.service;

import com.mihaiciobotaru.eshop.models.User;
import com.mihaiciobotaru.eshop.dto.UserDto;
import com.mihaiciobotaru.eshop.exception.UserAlreadyExistsException;
import com.mihaiciobotaru.eshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import com.mihaiciobotaru.eshop.models.Authority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User registerNewUser(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail()) ||
            userRepository.existsByUsername(userDto.getUsername())) {
            throw new UserAlreadyExistsException("Email or username already exists");
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(new BCryptPasswordEncoder().encode(userDto.getPassword()));
        user.setEnabled(true); // Assuming new users are enabled by default

        return userRepository.save(user);
    }

    public User linkToAuthority(User user, Authority authority) {
        user.setAuthority(authority);
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(userDetails.getUsername());
                    user.setEmail(userDetails.getEmail());

                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}