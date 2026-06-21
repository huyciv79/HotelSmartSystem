package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.BookingRoomAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRoomAccessRepository extends JpaRepository<BookingRoomAccess, Integer> {

    List<BookingRoomAccess> findByBookingid_IdOrderByRoomid_RoomnumberAsc(Integer bookingId);
}
