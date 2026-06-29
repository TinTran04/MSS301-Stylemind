package com.stylemind.user.repository;

import com.stylemind.user.entity.ProcessedEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
