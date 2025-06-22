package com.eshop.productcategoryservice;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class CategoryServiceApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(CategoryServiceApplication.class, args);
    }
    
}
