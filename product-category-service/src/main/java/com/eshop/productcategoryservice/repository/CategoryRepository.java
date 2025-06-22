package com.eshop.productcategoryservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eshop.productcategoryservice.models.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Category> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);
    Page<Category> findByNameAndDescription(String name, String description, Pageable pageable);
    Page<Category> findByNameOrDescription(String name, String description, Pageable pageable);
}
