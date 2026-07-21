package com.stylemind.order.repository;

import com.stylemind.order.entity.OrderItem;
import com.stylemind.order.dto.OrderItemCountResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    List<OrderItem> findByOrderId(String orderId);

    @Query("""
            SELECT new com.stylemind.order.dto.OrderItemCountResponse(i.orderId, COUNT(i))
            FROM OrderItem i
            WHERE i.orderId IN :orderIds
            GROUP BY i.orderId
            """)
    List<OrderItemCountResponse> countByOrderIds(@Param("orderIds") Collection<String> orderIds);
}
