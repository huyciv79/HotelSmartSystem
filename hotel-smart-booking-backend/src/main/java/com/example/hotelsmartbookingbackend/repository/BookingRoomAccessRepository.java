package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.BookingRoomAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRoomAccessRepository extends JpaRepository<BookingRoomAccess, Integer> {

    List<BookingRoomAccess> findByBookingid_IdOrderByRoomid_RoomnumberAsc(Integer bookingId);

    @org.springframework.data.jpa.repository.Query("""
        select bra
        from BookingRoomAccess bra
        left join fetch bra.roomid
        where bra.bookingid.id in :bookingIds
    """)
    List<BookingRoomAccess> findByBookingid_IdIn(@org.springframework.data.repository.query.Param("bookingIds") java.util.Collection<Integer> bookingIds);
}
