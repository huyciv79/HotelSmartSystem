package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.example.hotelsmartbookingbackend.enums.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Integer>, JpaSpecificationExecutor<Booking> {

    boolean existsByBookingReference(String bookingreference);


    @Query("SELECT b FROM Booking b WHERE b.createdAt >= :startDate AND b.status <> com.example.hotelsmartbookingbackend.enums.BookingStatus.CANCELLED")
    List<Booking> findActiveBookingsSince(@Param("startDate") Instant startDate);

    @Query("SELECT SUM(b.finalAmount) FROM Booking b WHERE b.status <> com.example.hotelsmartbookingbackend.enums.BookingStatus.CANCELLED")
    BigDecimal sumExpectedRevenue();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN (com.example.hotelsmartbookingbackend.enums.BookingStatus.CONFIRMED, com.example.hotelsmartbookingbackend.enums.BookingStatus.PAID, com.example.hotelsmartbookingbackend.enums.BookingStatus.PARTIALLY_PAID)")
    long countConfirmedBookings();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN (com.example.hotelsmartbookingbackend.enums.BookingStatus.CHECKED_IN, com.example.hotelsmartbookingbackend.enums.BookingStatus.STAYING)")
    long countCheckedInBookings();

    @Query("SELECT b FROM Booking b WHERE b.status <> com.example.hotelsmartbookingbackend.enums.BookingStatus.CANCELLED")
    List<Booking> findAllActiveBookings();
}