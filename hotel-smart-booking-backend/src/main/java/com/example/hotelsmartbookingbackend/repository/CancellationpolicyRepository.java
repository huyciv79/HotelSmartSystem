package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Integer> {
    List<CancellationPolicy> findByIsActiveTrueOrderByPriorityDescDaysBeforeCheckInDesc();
}
