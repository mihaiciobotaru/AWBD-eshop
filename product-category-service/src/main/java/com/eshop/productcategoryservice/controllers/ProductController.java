package com.eshop.productcategoryservice.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.eshop.productcategoryservice.exception.ResourceNotFoundException;
import com.eshop.productcategoryservice.models.Product;
import com.eshop.productcategoryservice.models.Category;
import com.eshop.productcategoryservice.service.ProductService;
import com.eshop.productcategoryservice.service.CategoryService;
import com.eshop.productcategoryservice.dto.ProductDto;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/list-all")
    public ResponseEntity<Page<Product>> listProducts(Model model,
                                @RequestParam(defaultValue = "0", required = false) int page,
                                @RequestParam(defaultValue = "6", required = false) int pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String sort) {

        logger.info("Received GET request for products list. Page: {}, Size: {}", page, pageSize);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Product> products;

        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length == 2) {
                String sortBy = sortParams[0];
                String sortDirection = sortParams[1].equalsIgnoreCase("desc") ? "DESC" : "ASC";
                if (sortBy.equals("price") && sortDirection.equals("ASC")) {
                    products = productService.findAllByOrderByPriceAsc(pageable);
                    logger.info("Sorting products by price in ascending order");
                } else if (sortBy.equals("price") && sortDirection.equals("DESC")) {
                    products = productService.findAllByOrderByPriceDesc(pageable);
                    logger.info("Sorting products by price in descending order");
                } else if (sortBy.equals("name") && sortDirection.equals("ASC")) {
                    products = productService.findAllByOrderByNameAsc(pageable);
                    logger.info("Sorting products by name in ascending order");
                } else if (sortBy.equals("name") && sortDirection.equals("DESC")) {
                    products = productService.findAllByOrderByNameDesc(pageable);
                    logger.info("Sorting products by name in descending order");
                } else {
                    products = productService.findAll(pageable);
                    logger.warn("Invalid sort parameters provided, listing all products");
                }
            } else {
                products = productService.findAll(pageable);
                logger.warn("Invalid sort parameters provided, listing all products");
            }
        }
        // if keyword and sort are provided, filter and sort products
        else if (keyword != null && !keyword.isEmpty()) {
            products = productService.findByNameContainingIgnoreCase(keyword, pageable);
            logger.info("Searching products with keyword: {}", keyword);
        } else {
            products = productService.findAll(pageable);
            logger.info("Listing all products");
        }

        logger.info("Listing the following products: {}", products.stream()
                .map(Product::getName)
                .toList());

        return ResponseEntity.ok(products);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getProductById(@RequestParam Long id) {
        logger.info("Received GET request for product with ID: {}", id);
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent()) {
            logger.info("Viewing product: {}", product.get().getName());
            return ResponseEntity.ok(product.get());
        } else {
            logger.error("Product with id {} not found", id);
            return new ResponseEntity<>(
                Map.of("error", "Product with id " + id + " not found"), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> addProductForm(@Valid @RequestBody ProductDto productDto, 
                                            BindingResult bindingResult) {
        logger.info("Received request to create product: {}", productDto);
        if (bindingResult.hasErrors()) {

            Map<String, List<String>> errors = bindingResult.getFieldErrors().stream()
            .collect(Collectors.groupingBy(
                    FieldError::getField,
                    Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));

            logger.warn("Validation errors for product creation: {}", errors);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Validation failed. Please check the 'errors' field for details.");
            errorResponse.put("errors", errors);
            errorResponse.put("expected_fields", List.of("name","price", "category_id"));

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        try {
            Optional<Category> category = categoryService.getCategoryById(productDto.getCategoryId());
            if (category.isEmpty()) {
                logger.error("Category not found with id: {}", productDto.getCategoryId());
                return new ResponseEntity<>(
                    Map.of("error", "Category with id " + productDto.getCategoryId() + " not found"), HttpStatus.NOT_FOUND);
            }
            Product product = new Product(productDto.getName(), productDto.getDescription(), 
                                           productDto.getPrice(), category.get());
            
            Product createdProduct = productService.createProduct(product);
            logger.info("Product created successfully: {}", createdProduct);
            return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating product: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("error", "Failed to create product"), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/edit/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id,
                                            @Valid @RequestBody Product product,
                                            BindingResult bindingResult) {
        logger.info("Received PUT request to update product with ID: {}", id);

        if (bindingResult.hasErrors()) {
            Map<String, List<String>> errors = bindingResult.getFieldErrors().stream()
            .collect(Collectors.groupingBy(
                    FieldError::getField,
                    Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));
            logger.warn("Validation errors for updating product with ID {}: {}", id, errors);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Validation failed for update. Please check the 'errors' field for details.");
            errorResponse.put("errors", errors);
            errorResponse.put("expected_fields", List.of("name","price", "category_id"));
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        try {
            categoryService.getCategoryById(product.getCategory().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + product.getCategory().getId()));
            Product updatedProduct = productService.updateProduct(id, product);
            logger.info("Product with ID {} updated successfully: {}", id, updatedProduct);
            return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            logger.warn("Attempted to update non-existent product with ID {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error updating product with ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(Map.of("error", "Failed to edit product"), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        logger.info("Received DELETE request for product with ID: {}", id);

        try {
            productService.deleteProduct(id);
            logger.info("Product with ID {} deleted successfully.", id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ResourceNotFoundException e) {

            logger.warn("Attempted to delete non-existent product with ID {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {

            logger.error("Error deleting product with ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(Map.of("error", "Failed to delete product"), HttpStatus.BAD_REQUEST);
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String errorMessage = "Request body is missing or a malformed JSON. Please provide a valid JSON payload.";
        logger.error("Caught HttpMessageNotReadableException: {}", ex);

        return new ResponseEntity<>(Map.of("error", errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("ResourceNotFoundException caught: {}", ex.getMessage());
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND); // 404 Not Found
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("An unexpected internal server error occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(Map.of("error", "An unexpected internal server error occurred."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}