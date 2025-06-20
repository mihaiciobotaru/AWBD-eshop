package com.eshop.categoryservice.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.eshop.categoryservice.exception.ResourceNotFoundException;
import com.eshop.categoryservice.models.Category;
import com.eshop.categoryservice.service.CategoryService;

import jakarta.validation.Valid;


@Controller
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listCategories(
            Model model,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "6", required = false) int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categoryPage;
        if (size <= 0) {
            categoryPage = categoryService.getAllCategories(PageRequest.of(0, 10)); // Default to first page with 10 items
        } else {
            categoryPage = categoryService.getAllCategories(pageable);
        }
        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        return "category/list";
    }

    @GetMapping("/{id}")
    public String getCategory(@PathVariable Long id, Model model) {
        Optional<Category> category = categoryService.getCategoryById(id);
        if (category.isEmpty()) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        model.addAttribute("category", category.get());
        return "category/detail";
    }

    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("category") Category category) {
        try {
            categoryService.saveCategory(category);
            return "Category created successfully";
        } catch (Exception e) {
            return "Error creating category: " + e.getMessage();
        }
            return "category/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        Optional<Category> category = categoryService.getCategoryById(id);
        if (category.isEmpty()) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        model.addAttribute("category", category.get());
        return "category/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateCategory(@PathVariable Long id, 
                                 @Valid @ModelAttribute("category") Category category, 
                                 BindingResult result, 
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "category/edit";
        }
        if (!categoryService.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryService.updateCategory(id, category);
        redirectAttributes.addFlashAttribute("message", "Category updated successfully");
        return "redirect:/api/categories";
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!categoryService.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryService.deleteCategory(id);
        redirectAttributes.addFlashAttribute("message", "Category deleted successfully");
        return "redirect:/api/categories";
    }
    
}
