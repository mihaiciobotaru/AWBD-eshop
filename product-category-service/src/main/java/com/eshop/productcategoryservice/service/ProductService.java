package com.eshop.productcategoryservice.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.eshop.productcategoryservice.exception.ResourceNotFoundException;
import com.eshop.productcategoryservice.models.Product;
import com.eshop.productcategoryservice.repository.ProductRepository;

import jakarta.transaction.Transactional;


@Service
public class ProductService {
    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product product) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        return productRepository.save(existingProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productRepository.delete(product);
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Page<Product> getProductsByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    public Page<Product> getProductsByName(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    public Page<Product> getProductsByPriceRange(Double minPrice, Double maxPrice, Pageable pageable) {
        return productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
    }

    public Page<Product> getProductsByCategoryIdAndPriceRange(Long categoryId, Double minPrice, Double maxPrice, Pageable pageable) {
        return productRepository.findByCategoryIdAndPriceBetween(categoryId, minPrice, maxPrice, pageable);
    }

    public Page<Product> findAllByOrderByPriceAsc(Pageable pageable) {
        return productRepository.findAllByOrderByPriceAsc(pageable);
    }

    public Page<Product> findAllByOrderByPriceDesc(Pageable pageable) {
        return productRepository.findAllByOrderByPriceDesc(pageable);
    }

    public Page<Product> findAllByOrderByNameAsc(Pageable pageable) {
        return productRepository.findAllByOrderByNameAsc(pageable);
    }

    public Page<Product> findAllByOrderByNameDesc(Pageable pageable) {
        return productRepository.findAllByOrderByNameDesc(pageable);
    }

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable);
    }

}

