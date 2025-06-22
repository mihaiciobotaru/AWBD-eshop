package com.eshop.authservice.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eshop.authservice.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Page<User> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);
    Page<User> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);    
}
