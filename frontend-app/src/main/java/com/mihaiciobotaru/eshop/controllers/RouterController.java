package com.mihaiciobotaru.eshop.controllers;

// i want to have following pages /products /cart /product-form
// i want regular users to access /products and /cart
// i want admin users to access /products and /cart and /product-form
// everything else should be blocked

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RouterController {



    @GetMapping("/products")
    public String productsPage() {
        return "products/list";
    }

    @GetMapping("/cart")
    public String cartPage() {
        return "cart/view";
    }

    @GetMapping("/product-form")
    public String productFormPage() {
        return "products/form";
    }

}