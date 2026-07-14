package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.BookingRoomAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRoomAccessRepository extends JpaRepository<BookingRoomAccess, Integer> {

    List<BookingRoomAccess> findByBooking_IdOrderByRoom_RoomNumberAsc(Integer bookingId);

    @org.springframework.data.jpa.repository.Query("""
        select bra
        from BookingRoomAccess bra
        left join fetch bra.room
        where bra.booking.id in :bookingIds
    """)
    List<BookingRoomAccess> findByBooking_IdIn(@org.springframework.data.repository.query.Param("bookingIds") java.util.Collection<Integer> bookingIds);
}
