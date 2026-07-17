package com.stylemind.cart.repository;

import com.stylemind.cart.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, String> {
    Optional<ShoppingCart> findByUserId(String userId);

    @Modifying
    @Query(value = "INSERT INTO shopping_carts (id, user_id, created_at, updated_at) "
            + "VALUES (:cartId, :userId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            + "ON CONFLICT (user_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("cartId") String cartId, @Param("userId") String userId);
}
