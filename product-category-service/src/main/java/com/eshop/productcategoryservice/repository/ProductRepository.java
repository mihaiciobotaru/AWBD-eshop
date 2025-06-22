package com.eshop.productcategoryservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eshop.productcategoryservice.models.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);
    Page<Product> findByCategoryIdAndPriceBetween(Long categoryId, Double minPrice, Double maxPrice, Pageable pageable);
    
    Page<Product> findAllByOrderByPriceAsc(Pageable pageable);
    Page<Product> findAllByOrderByPriceDesc(Pageable pageable);

    Page<Product> findAllByOrderByNameAsc(Pageable pageable);
    Page<Product> findAllByOrderByNameDesc(Pageable pageable);

}
