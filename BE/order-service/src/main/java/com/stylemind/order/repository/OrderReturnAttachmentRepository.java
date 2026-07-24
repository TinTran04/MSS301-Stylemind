package com.stylemind.order.repository;

import com.stylemind.order.entity.OrderReturnAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderReturnAttachmentRepository extends JpaRepository<OrderReturnAttachment, String> {
    List<OrderReturnAttachment> findByReturnRequestIdOrderByCreatedAtAsc(String returnRequestId);
    List<OrderReturnAttachment> findByReturnRequestIdInOrderByCreatedAtAsc(Collection<String> returnRequestIds);
}
