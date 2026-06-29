package com.stylemind.notification.repository;

import com.stylemind.notification.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserId(String userId);
    Page<NotificationLog> findByUserId(String userId, Pageable pageable);
    List<NotificationLog> findByStatus(String status);

    @Query("""
            SELECT n FROM NotificationLog n
            WHERE (:userId IS NULL OR n.userId = :userId)
              AND (:status IS NULL OR n.status = :status)
              AND (:type IS NULL OR n.type = :type)
            """)
    Page<NotificationLog> search(
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("type") String type,
            Pageable pageable
    );
}
