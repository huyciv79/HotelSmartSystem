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

public interface BookingRepository extends JpaRepository<Booking, Integer>, JpaSpecificationExecutor<Booking> {

    boolean existsByBookingreference(String bookingreference);

    Page<Booking> findByStatus(String status, Pageable pageable);

    Page<Booking> findByBookingreference(String bookingReference, Pageable pageable);

    Page<Booking> findByUseridEmailContaining(String email, Pageable pageable);

    Page<Booking> findByCreatedatBetween( Instant from, Instant to, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.createdat >= :startDate AND b.status <> 'Cancelled'")
    List<Booking> findActiveBookingsSince(@Param("startDate") Instant startDate);

    @Query("SELECT SUM(b.finalamount) FROM Booking b WHERE b.status <> 'Cancelled'")
    BigDecimal sumExpectedRevenue();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN ('Confirmed', 'Paid', 'Partially Paid')")
    long countConfirmedBookings();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN ('Checked-in', 'Checked In', 'Staying')")
    long countCheckedInBookings();

    @Query("SELECT b FROM Booking b WHERE b.status <> 'Cancelled'")
    List<Booking> findAllActiveBookings();
}