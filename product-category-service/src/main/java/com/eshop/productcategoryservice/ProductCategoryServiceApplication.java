package com.eshop.productcategoryservice;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ProductCategoryServiceApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(ProductCategoryServiceApplication.class, args);
    }
    
}
