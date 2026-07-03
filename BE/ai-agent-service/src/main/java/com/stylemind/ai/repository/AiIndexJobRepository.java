package com.stylemind.ai.repository;

import com.stylemind.ai.entity.AiIndexJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AiIndexJobRepository extends JpaRepository<AiIndexJob, String> {

    @Query("""
            SELECT j FROM AiIndexJob j
            WHERE (CAST(:status AS string) IS NULL OR j.status = :status)
              AND (CAST(:targetType AS string) IS NULL OR j.targetType = :targetType)
            """)
    Page<AiIndexJob> search(@Param("status") String status, @Param("targetType") String targetType, Pageable pageable);
}
