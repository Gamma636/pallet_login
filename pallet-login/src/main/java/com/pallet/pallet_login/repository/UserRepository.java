package com.pallet.pallet_login.repository;

import com.pallet.pallet_login.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByUserName(String userName);

    // Case-insensitive match
    UserEntity findByUserNameIgnoreCase(String userName);

    // Custom query for more flexible matching (optional)
    @Query("SELECT u FROM UserEntity u WHERE TRIM(LOWER(u.userName)) = TRIM(LOWER(:username))")
    UserEntity findByUserNameFlexible(@Param("username") String username);

    // Get all usernames for debugging
    @Query("SELECT u.userName FROM UserEntity u")
    List<String> findAllUserNames();

    // Check if username exists (case-insensitive)
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE LOWER(TRIM(u.userName)) = LOWER(TRIM(:username))")
    boolean existsByUserNameIgnoreCase(@Param("username") String username);
}
