package com.stylemind.order.repository;

import com.stylemind.order.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, String> {

    List<ReturnItem> findByReturnRequestId(String returnRequestId);

    @Query("SELECT SUM(ri.quantity) FROM ReturnItem ri JOIN ri.returnRequest rr " +
           "WHERE rr.orderId = :orderId AND ri.orderItemId = :orderItemId " +
           "AND rr.status NOT IN ('REJECTED', 'CANCELLED', 'QC_FAILED')")
    Integer sumReservedOrConsumedQuantity(@Param("orderId") String orderId, @Param("orderItemId") String orderItemId);
}
