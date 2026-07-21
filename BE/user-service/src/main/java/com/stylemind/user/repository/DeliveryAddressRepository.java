package com.stylemind.user.repository;

import com.stylemind.user.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, String> {
    List<DeliveryAddress> findByUserId(String userId);
    Optional<DeliveryAddress> findByUserIdAndIsDefaultTrue(String userId);

    /**
     * Serializes default-address changes for one user, including the case where
     * the user has no address row to lock yet. PostgreSQL releases this lock at
     * the end of the surrounding transaction.
     */
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtext(:userId))", nativeQuery = true)
    Integer lockDefaultAddressSlot(@Param("userId") String userId);

    /**
     * Atomically clears the default flag on all addresses for a user.
     * Use before setting a new default to avoid the read-then-write race condition.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE DeliveryAddress a SET a.isDefault = false WHERE a.userId = :userId")
    void clearAllDefaultsByUserId(@Param("userId") String userId);
}
