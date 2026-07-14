package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByBooking_Id(Integer bookingId);
}
