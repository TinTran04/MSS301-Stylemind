package com.stylemind.user.repository;

import com.stylemind.user.entity.Address;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdOrderByDefaultAddressDescCreatedAtAsc(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Address a where a.userId = :userId")
    List<Address> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Address a where a.id = :id and a.userId = :userId")
    Optional<Address> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}
