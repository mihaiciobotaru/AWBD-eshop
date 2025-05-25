package com.mihaiciobotaru.eshop.repository;

import com.mihaiciobotaru.eshop.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Category> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);
    Page<Category> findByNameAndDescription(String name, String description, Pageable pageable);
    Page<Category> findByNameOrDescription(String name, String description, Pageable pageable);
}
