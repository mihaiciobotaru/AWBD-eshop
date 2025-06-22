package com.eshop.productcategoryservice.controllers;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.eshop.productcategoryservice.exception.ResourceNotFoundException;
import com.eshop.productcategoryservice.models.Category;
import com.eshop.productcategoryservice.models.Product;
import com.eshop.productcategoryservice.repository.ProductRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/api/products")
public class ProductController {
    private final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductRepository productRepository;

    @Autowired
    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping
    public String listProducts(Model model, 
                                @RequestParam(defaultValue = "0", required = false) int page,
                                @RequestParam(defaultValue = "6", required = false) int pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String sort) {

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Product> products;

        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length == 2) {
                String sortBy = sortParams[0];
                String sortDirection = sortParams[1].equalsIgnoreCase("desc") ? "DESC" : "ASC";
                if (sortBy.equals("price") && sortDirection.equals("ASC")) {
                    products = productRepository.findAllByOrderByPriceAsc(pageable);
                    logger.info("Sorting products by price in ascending order");
                } else if (sortBy.equals("price") && sortDirection.equals("DESC")) {
                    products = productRepository.findAllByOrderByPriceDesc(pageable);
                    logger.info("Sorting products by price in descending order");
                } else if (sortBy.equals("name") && sortDirection.equals("ASC")) {
                    products = productRepository.findAllByOrderByNameAsc(pageable);
                    logger.info("Sorting products by name in ascending order");
                } else if (sortBy.equals("name") && sortDirection.equals("DESC")) {
                    products = productRepository.findAllByOrderByNameDesc(pageable);
                    logger.info("Sorting products by name in descending order");
                } else {
                    products = productRepository.findAll(pageable);
                    logger.warn("Invalid sort parameters provided, listing all products");
                }
            }else {
                products = productRepository.findAll(pageable);
                logger.warn("Invalid sort parameters provided, listing all products");
            }
        }
        // if keyword and sort are provided, filter and sort products
        else if (keyword != null && !keyword.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCase(keyword, pageable);
            logger.info("Searching products with keyword: {}", keyword);
        } else {
            products = productRepository.findAll(pageable);
            logger.info("Listing all products");
        }
        model.addAttribute("products", products);
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);
        logger.info("Listing the following products: {}", products.stream()
                .map(Product::getName)
                .toList());

        return "products/list";
    }

    @GetMapping("/{id}")
    public String getProduct(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->{
                    logger.error("Product not found with id: {}", id);
                    return new ResourceNotFoundException("Product not found");
                });
        model.addAttribute("product", product);
        logger.info("Viewing product: {}", product.getName());
        return "products/view";
    }

    @GetMapping("/create")
    public String addProductForm(Model model) {
        List<Category> categories = Arrays.asList(
            restTemplate.getForObject("http://localhost:8082/api/categories", Category[].class)
        );
        model.addAttribute("categories", categories);
        return "products/product-form";
    }

    @PostMapping("/create")
    public String addProductForm(@Valid @ModelAttribute Product product, BindingResult bindingResult, 
                            Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            logger.warn("Product creation failed due to validation errors: {}", bindingResult.getAllErrors());
            return "products/product-form";
        }
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Product added successfully");
        logger.info("Product created successfully: {}", product.getName());
        return "redirect:/api/products";
    }

    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Product not found with id: {}", id);
                    return new ResourceNotFoundException("Product not found");
                });
        model.addAttribute("product", product);
        List<Category> categories = Arrays.asList(
            restTemplate.getForObject("http://localhost:8082/api/categories", Category[].class)
        );
        model.addAttribute("categories", categories);
        logger.info("Editing product: {}", product.getName());
        return "products/product-form";
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(@PathVariable Long id, 
                                @Valid @ModelAttribute Product product, 
                                BindingResult bindingResult, 
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            logger.warn("Product update failed due to validation errors: {}", bindingResult.getAllErrors());
            return "products/product-form";
        }
        if (!productRepository.existsById(id)) {
            logger.error("Product not found with id: {}", id);
            throw new ResourceNotFoundException("Product not found");
        }
        product.setId(id);
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully");
        logger.info("Product updated successfully: {}", product.getName());
        return "redirect:/api/products";
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        logger.info("Product with id {} deleted successfully", id);
        return "redirect:/products";
    }
}