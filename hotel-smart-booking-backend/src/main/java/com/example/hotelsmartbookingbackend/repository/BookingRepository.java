package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;

public interface BookingRepository extends JpaRepository<Booking, Integer>, JpaSpecificationExecutor<Booking> {

    boolean existsByBookingreference(String bookingreference);

    Page<Booking> findByStatus(String status, Pageable pageable);

    Page<Booking> findByBookingreference(String bookingReference, Pageable pageable);

    Page<Booking> findByUseridEmailContaining(String email, Pageable pageable);

    Page<Booking> findByCreatedatBetween( Instant from, Instant to, Pageable pageable);
}