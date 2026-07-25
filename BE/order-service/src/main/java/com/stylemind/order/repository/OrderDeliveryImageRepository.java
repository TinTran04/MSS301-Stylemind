package com.stylemind.order.repository;

import com.stylemind.order.entity.OrderDeliveryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDeliveryImageRepository extends JpaRepository<OrderDeliveryImage, String> {
    List<OrderDeliveryImage> findByOrderIdOrderByCreatedAtDesc(String orderId);
    long countByOrderId(String orderId);
}
