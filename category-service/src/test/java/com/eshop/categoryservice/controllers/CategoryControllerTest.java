package com.eshop.categoryservice.controllers;

import com.eshop.categoryservice.exception.ResourceNotFoundException;
import com.eshop.categoryservice.models.Category;
import com.eshop.categoryservice.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


// [ERROR]   CategoryControllerTest.testCreateCategory_validationError:184 Status expected:<400> but was:<500>
// [ERROR]   CategoryControllerTest.testDeleteCategory_internalServerError:314 Status expected:<500> but was:<400>
// [ERROR]   CategoryControllerTest.testUpdateCategory_validationError:262 Status expected:<400> but was:<500>


@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        category1 = new Category();
        category1.setId(1L);
        category1.setName("Electronics");

        category2 = new Category();
        category2.setId(2L);
        category2.setName("Books");
    }

    // --- GET /list-all ---
    @Test
    void testListCategories_success_returnsPagedCategories() throws Exception {
        List<Category> categories = Arrays.asList(category1, category2);
        Page<Category> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 10), 2);

        when(categoryService.getAllCategories(any(Pageable.class))).thenReturn(categoryPage);

        mockMvc.perform(get("/list-all")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Electronics")))
                .andExpect(jsonPath("$.content[1].id", is(2)))
                .andExpect(jsonPath("$.content[1].name", is("Books")))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(1)))
                .andExpect(jsonPath("$.page.number", is(0))) // current page number
                .andExpect(jsonPath("$.page.size", is(10))); // page size
    }

    @Test
    void testListCategories_invalidSize_defaultsToTen() throws Exception {
        List<Category> categories = Collections.singletonList(category1);
        Page<Category> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 10), 1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(categoryService.getAllCategories(pageableCaptor.capture())).thenReturn(categoryPage);

        mockMvc.perform(get("/list-all")
                .param("page", "0")
                .param("size", "0") // Invalid size
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // Verify that the service was called with a Pageable of size 10
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageSize(), is(10));
    }


    // --- GET /get?id={id} ---
    @Test
    void testGetCategoryById_success() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(Optional.of(category1));

        mockMvc.perform(get("/get")
                .param("id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Electronics")));
    }

    @Test
    void testGetCategoryById_notFound() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/get")
                .param("id", "99")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Category with id 99 not found")));
    }


    // --- POST /create ---
    @Test
    void testCreateCategory_success() throws Exception {
        Category newCategory = new Category();
        newCategory.setName("New Category");

        Category savedCategory = new Category();
        savedCategory.setId(3L);
        savedCategory.setName("New Category");

        when(categoryService.createCategory(any(Category.class))).thenReturn(savedCategory);

        mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("New Category")));
    }

    @Test
    void testCreateCategory_validationError() throws Exception {
        Category invalidCategory = new Category();
        invalidCategory.setName(""); // Assuming @NotBlank or @NotEmpty validation

        mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCategory)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed. Please check the 'errors' field for details.")))
                .andExpect(jsonPath("$.errors.name", notNullValue())); // Expecting a validation error for 'name'
    }

    @Test
    void testCreateCategory_businessError() throws Exception {
        Category existingCategory = new Category();
        existingCategory.setName("Electronics");

        when(categoryService.createCategory(any(Category.class)))
                .thenThrow(new IllegalArgumentException("Failed to create category"));

        mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(existingCategory)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Failed to create category")));
    }

    @Test
    void testCreateCategory_malformedJson() throws Exception {
        String malformedJson = "{name: \"Test\", }"; // Missing quotes around key, trailing comma

        mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Request body is missing or a malformed JSON. Please provide a valid JSON payload.")));
    }


    // --- PUT /edit/{id} ---
    @Test
    void testUpdateCategory_success() throws Exception {
        Long categoryId = 1L;
        Category updatedCategoryDetails = new Category();
        updatedCategoryDetails.setName("Updated Electronics");

        Category updatedCategory = new Category();
        updatedCategory.setId(categoryId);
        updatedCategory.setName("Updated Electronics");

        when(categoryService.updateCategory(eq(categoryId), any(Category.class))).thenReturn(updatedCategory);

        mockMvc.perform(put("/edit/{id}", categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCategoryDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(categoryId.intValue())))
                .andExpect(jsonPath("$.name", is("Updated Electronics")));
    }

    @Test
    void testUpdateCategory_notFound() throws Exception {
        Long nonExistentId = 99L;
        Category updatePayload = new Category();
        updatePayload.setName("Non Existent Category");

        when(categoryService.updateCategory(eq(nonExistentId), any(Category.class)))
                .thenThrow(new ResourceNotFoundException("Category not found with ID: " + nonExistentId));

        mockMvc.perform(put("/edit/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Category not found with ID: " + nonExistentId)));
    }

    @Test
    void testUpdateCategory_validationError() throws Exception {
        Long categoryId = 1L;
        Category invalidUpdatePayload = new Category();
        invalidUpdatePayload.setName(""); // Invalid name

        mockMvc.perform(put("/edit/{id}", categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUpdatePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed for update. Please check the 'errors' field for details.")))
                .andExpect(jsonPath("$.errors.name", notNullValue()));
    }

    @Test
    void testUpdateCategory_businessError() throws Exception {
        Long categoryId = 1L;
        Category duplicateNamePayload = new Category();
        duplicateNamePayload.setName("Books"); // Name already exists (category2)

        when(categoryService.updateCategory(eq(categoryId), any(Category.class)))
                .thenThrow(new IllegalArgumentException("Failed to edit category"));

        mockMvc.perform(put("/edit/{id}", categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateNamePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Failed to edit category")));
    }


    // --- DELETE /delete/{id} ---
    @Test
    void testDeleteCategory_success() throws Exception {
        Long categoryId = 1L;
        doNothing().when(categoryService).deleteCategory(categoryId); // Service returns void

        mockMvc.perform(delete("/delete/{id}", categoryId))
                .andExpect(status().isNoContent()); // 204 No Content
        
        verify(categoryService, times(1)).deleteCategory(categoryId); // Verify service method was called
    }

    @Test
    void testDeleteCategory_notFound() throws Exception {
        Long nonExistentId = 99L;
        doThrow(new ResourceNotFoundException("Category not found with ID: " + nonExistentId))
                .when(categoryService).deleteCategory(nonExistentId);

        mockMvc.perform(delete("/delete/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Category not found with ID: " + nonExistentId)));
    }

}
