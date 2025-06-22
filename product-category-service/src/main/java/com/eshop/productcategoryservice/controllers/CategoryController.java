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
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.eshop.productcategoryservice.exception.ResourceNotFoundException;
import com.eshop.productcategoryservice.models.Category;
import com.eshop.productcategoryservice.service.CategoryService;

import jakarta.validation.Valid;


@Controller
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list-all")
    public ResponseEntity<Page<Category>> listCategories(
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size) {
        
        logger.info("Received GET request for categories list. Page: {}, Size: {}", page, size);
        Pageable pageable;
        if (size <= 0) {

            pageable = PageRequest.of(page, 10);
            logger.warn("Invalid page size ({}). Defaulting to 10 for pagination.", size);
        } else {
            pageable = PageRequest.of(page, size);
        }

        Page<Category> categoryPage = categoryService.getAllCategories(pageable);
        logger.info("Retrieved {} categories (Page {} of {}).",
                    categoryPage.getNumberOfElements(), categoryPage.getNumber(), categoryPage.getTotalPages());
        
        return ResponseEntity.ok(categoryPage);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getCategoryById(@RequestParam Long id) {
        logger.info("Received request to get category by ID: {}", id);
        Optional<Category> category = categoryService.getCategoryById(id);
        if (category.isEmpty()) {
            logger.info("Category not found with id: {}", id);
            return new ResponseEntity<>(
                    Map.of("error", "Category with id " + id + " not found"), HttpStatus.NOT_FOUND);
        }
        logger.info("Category found: {}", category.get());
        return ResponseEntity.ok(category.get());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCategory(@Valid @RequestBody Category category,
            BindingResult bindingResult) {
        logger.info("Received request to create category: {}", category);
        if (bindingResult.hasErrors()) {

            Map<String, List<String>> errors = bindingResult.getFieldErrors().stream()
            .collect(Collectors.groupingBy(
                    FieldError::getField,
                    Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));

            logger.warn("Validation errors for category creation: {}", errors);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Validation failed. Please check the 'errors' field for details.");
            errorResponse.put("errors", errors);
            errorResponse.put("expected_fields", List.of("name"));

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        try {
            Category createdCategory = categoryService.createCategory(category);
            logger.info("Category created successfully: {}", createdCategory);
            return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating category: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("error", "Failed to create category"), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id,
                                            @Valid @RequestBody Category category,
                                            BindingResult bindingResult) {
        logger.info("Received PUT request to update category with ID: {}", id);

        if (bindingResult.hasErrors()) {
            Map<String, List<String>> errors = bindingResult.getFieldErrors().stream()
            .collect(Collectors.groupingBy(
                    FieldError::getField,
                    Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));
            logger.warn("Validation errors for updating category with ID {}: {}", id, errors);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Validation failed for update. Please check the 'errors' field for details.");
            errorResponse.put("errors", errors);
            errorResponse.put("expected_fields", List.of("name"));
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        try {
            Category updatedCategory = categoryService.updateCategory(id, category);
            logger.info("Category with ID {} updated successfully: {}", id, updatedCategory);
            return new ResponseEntity<>(updatedCategory, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            logger.warn("Attempted to update non-existent category with ID {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error updating category with ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(Map.of("error", "Failed to edit category"), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        logger.info("Received DELETE request for category with ID: {}", id);

        try {
            categoryService.deleteCategory(id);
            logger.info("Category with ID {} deleted successfully.", id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ResourceNotFoundException e) {

            logger.warn("Attempted to delete non-existent category with ID {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {

            logger.error("Error deleting category with ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(Map.of("error", "Failed to delete category"), HttpStatus.BAD_REQUEST);
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
