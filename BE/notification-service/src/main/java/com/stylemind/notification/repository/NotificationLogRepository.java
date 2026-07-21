package com.stylemind.notification.repository;

import com.stylemind.notification.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserId(String userId);
    @Query("""
            SELECT n FROM NotificationLog n
            WHERE n.userId = :userId
            ORDER BY COALESCE(n.sentAt, n.createdAt) DESC, n.id DESC
            """)
    List<NotificationLog> findByUserIdNewestFirst(@Param("userId") String userId);
    Page<NotificationLog> findByUserId(String userId, Pageable pageable);
    List<NotificationLog> findByStatus(String status);
    long countByStatus(String status);
    Optional<NotificationLog> findByIdAndUserId(Long id, String userId);

    @Query(
            value = """
            SELECT n FROM NotificationLog n
            WHERE (:userId IS NULL OR n.userId = :userId)
              AND (:status IS NULL OR n.status = :status)
              AND (:type IS NULL OR n.type = :type)
            ORDER BY COALESCE(n.sentAt, n.createdAt) DESC, n.id DESC
            """,
            countQuery = """
            SELECT COUNT(n) FROM NotificationLog n
            WHERE (:userId IS NULL OR n.userId = :userId)
              AND (:status IS NULL OR n.status = :status)
              AND (:type IS NULL OR n.type = :type)
            """
    )
    Page<NotificationLog> search(
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("type") String type,
            Pageable pageable
    );
}
