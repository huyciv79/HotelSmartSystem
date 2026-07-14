package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.CustomerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRequestRepository extends JpaRepository<CustomerRequest, Integer> {
    List<CustomerRequest> findByBooking_Id(Integer bookingId);
    List<CustomerRequest> findByRequestTypeAndStatusOrderByCreatedAtDesc(String requestType, String status);
}
