package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByIdCardNumber(String idCardNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    java.util.List<User> findAllByRoleIn(java.util.Collection<com.example.hotelsmartbookingbackend.enums.Role> roles);
}
