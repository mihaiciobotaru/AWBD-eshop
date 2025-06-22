package com.eshop.authservice;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AuthServiceApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(AuthServiceApplication.class, args);
    }
    
}
