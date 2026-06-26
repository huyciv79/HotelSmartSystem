package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Cancellationpolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationpolicyRepository extends JpaRepository<Cancellationpolicy, Integer> {
    List<Cancellationpolicy> findByIsactiveTrueOrderByPriorityDescDaysbeforecheckinDesc();
}
