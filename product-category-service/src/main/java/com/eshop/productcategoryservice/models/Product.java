package com.eshop.productcategoryservice.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name cannot be blank.")
    @jakarta.persistence.Column(unique = true)
    @Size(min = 3, max = 255, message = "Product name must be between 3 and 255 characters.")
    private String name;

    @Lob
    private String description;

    @NotNull(message = "Product price cannot be null.")
    @DecimalMin(value = "0.00", inclusive = false, message = "Product price must be greater than 0.")
    private float price;

    @ManyToOne
    @NotNull(message = "Product must belong to a category.")
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public Product() {
    }
    public Product(String name, String description, float price, Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    public Product(String name, String description, float price, Long categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = new Category();
        this.category.setId(categoryId);
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public float getPrice() {
        return price;
    }
    public void setPrice(float price) {
        this.price = price;
    }
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", category=" + category +
                '}';
    }
}