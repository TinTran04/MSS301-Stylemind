package com.stylemind.notification.repository;

import com.stylemind.notification.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserId(String userId);
    Page<NotificationLog> findByUserId(String userId, Pageable pageable);
    List<NotificationLog> findByStatus(String status);
}