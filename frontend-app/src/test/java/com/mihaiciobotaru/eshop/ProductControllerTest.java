package com.mihaiciobotaru.eshop;

import com.mihaiciobotaru.eshop.exception.ResourceNotFoundException;
import com.mihaiciobotaru.eshop.models.Product;
import com.mihaiciobotaru.eshop.repository.CategoryRepository;
import com.mihaiciobotaru.eshop.repository.ProductRepository;
import com.mihaiciobotaru.eshop.controllers.ProductController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private Model model;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listProducts_withSortByPriceAsc() {
        Page<Product> page = new PageImpl<>(List.of(new Product()));
        when(productRepository.findAllByOrderByPriceAsc(any(Pageable.class))).thenReturn(page);

        String view = productController.listProducts(model, 0, 6, null, "price,asc");

        assertEquals("products/list", view);
        verify(model).addAttribute(eq("products"), eq(page));
    }

    @Test
    void listProducts_withSortByNameDesc() {
        Page<Product> page = new PageImpl<>(List.of(new Product()));
        when(productRepository.findAllByOrderByNameDesc(any(Pageable.class))).thenReturn(page);

        String view = productController.listProducts(model, 0, 6, null, "name,desc");

        assertEquals("products/list", view);
        verify(model).addAttribute(eq("products"), eq(page));
    }

    @Test
    void listProducts_withKeyword() {
        Page<Product> page = new PageImpl<>(List.of(new Product()));
        when(productRepository.findByNameContainingIgnoreCase(eq("test"), any(Pageable.class))).thenReturn(page);

        String view = productController.listProducts(model, 0, 6, "test", null);

        assertEquals("products/list", view);
        verify(model).addAttribute(eq("products"), eq(page));
    }

    @Test
    void listProducts_default() {
        Page<Product> page = new PageImpl<>(List.of(new Product()));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        String view = productController.listProducts(model, 0, 6, null, null);

        assertEquals("products/list", view);
        verify(model).addAttribute(eq("products"), eq(page));
    }

    @Test
    void getProduct_found() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        String view = productController.getProduct(1L, model);

        assertEquals("products/view", view);
        verify(model).addAttribute("product", product);
    }

    @Test
    void getProduct_notFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productController.getProduct(1L, model));
    }

    @Test
    void addProductForm_post_valid() {
        Product product = new Product();
        when(bindingResult.hasErrors()).thenReturn(false);

        String view = productController.addProductForm(product, bindingResult, model, redirectAttributes);

        assertEquals("redirect:/api/products", view);
        verify(productRepository).save(product);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    void addProductForm_post_invalid() {
        Product product = new Product();
        when(bindingResult.hasErrors()).thenReturn(true);

        String view = productController.addProductForm(product, bindingResult, model, redirectAttributes);

        assertEquals("products/product-form", view);
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_valid() {
        Product product = new Product();
        product.setName("Test");
        when(bindingResult.hasErrors()).thenReturn(false);
        when(productRepository.existsById(1L)).thenReturn(true);

        String view = productController.updateProduct(1L, product, bindingResult, redirectAttributes);

        assertEquals("redirect:/api/products", view);
        verify(productRepository).save(product);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        assertEquals(1L, product.getId());
    }

    @Test
    void updateProduct_invalid() {
        Product product = new Product();
        when(bindingResult.hasErrors()).thenReturn(true);

        String view = productController.updateProduct(1L, product, bindingResult, redirectAttributes);

        assertEquals("products/product-form", view);
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct() {
        String view = productController.deleteProduct(1L);

        assertEquals("redirect:/products", view);
        verify(productRepository).deleteById(1L);
    }
}