package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Bookingservice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingserviceRepository extends JpaRepository<Bookingservice, Integer> {
    List<Bookingservice> findByBookingid_Id(Integer bookingId);
}
