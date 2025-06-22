package com.eshop.shoppingcartservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eshop.shoppingcartservice.models.ShoppingCart;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
   Optional<ShoppingCart> findByUser_Id(Long userId);
   void deleteByUser_Id(Long userId);
}
