package com.mihaiciobotaru.eshop.repository;

import com.mihaiciobotaru.eshop.models.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Long> {
    Optional<Authority> findByUserId(Long userId);
}