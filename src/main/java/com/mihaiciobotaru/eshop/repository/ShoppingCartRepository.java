package com.mihaiciobotaru.eshop.repository;

import com.mihaiciobotaru.eshop.models.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
   Optional<ShoppingCart> findByUser_Id(Long userId);
   void deleteByUser_Id(Long userId);
}
