package com.stylemind.user.repository;

import com.stylemind.user.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, String> {
    List<DeliveryAddress> findByUserId(String userId);

    Optional<DeliveryAddress> findByIdAndUserId(String id, String userId);

    Optional<DeliveryAddress> findByUserIdAndIsDefaultTrue(String userId);
}
