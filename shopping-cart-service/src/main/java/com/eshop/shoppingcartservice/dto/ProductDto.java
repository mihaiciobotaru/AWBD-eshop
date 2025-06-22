package com.eshop.shoppingcartservice.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductDto {
    @NotNull(message = "Product ID cannot be null.")
    @Min(value = 1, message = "Product ID must be a positive number.")
    private Long id;

    @NotBlank(message = "Product name cannot be blank.")
    @Size(min = 3, max = 255, message = "Product name must be between 3 and 255 characters.")
    private String name;

    @Lob
    private String description;

    @NotNull(message = "Product price cannot be null.")
    @DecimalMin(value = "0.00", inclusive = false, message = "Product price must be greater than 0.")
    private float price;

    @NotNull(message = "Category ID cannot be null.")
    @Min(value = 1, message = "Category ID must be a positive number.")
    private Long categoryId;

    @Override
    public String toString() {
        return "ProductDto{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", categoryId=" + categoryId +
                '}';
    }
}