package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.BookingService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingServiceRepository extends JpaRepository<BookingService, Integer> {
    List<BookingService> findByBooking_Id(Integer bookingId);
}
