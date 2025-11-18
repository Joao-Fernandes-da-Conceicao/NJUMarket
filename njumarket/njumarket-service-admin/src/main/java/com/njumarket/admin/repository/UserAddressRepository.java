package com.njumarket.admin.repository;

import com.njumarket.admin.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, String> {

    List<UserAddress> findByUserId(String userId);

    List<UserAddress> findByUserIdAndIsActive(String userId, Boolean isActive);

    Optional<UserAddress> findByAddressIdAndUserId(String addressId, String userId);

    @Modifying
    @Query("update UserAddress ua set ua.isDefault = false where ua.userId = :userId and ua.isDefault = true")
    void clearDefaultAddress(String userId);
}

