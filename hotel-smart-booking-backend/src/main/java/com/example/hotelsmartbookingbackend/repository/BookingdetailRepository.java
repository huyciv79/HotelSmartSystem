package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.BookingDetail;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {

    Optional<BookingDetail> findByBooking_Id(Integer bookingId);

    boolean existsByQrCodeValue(String qrCodeValue);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d
            from BookingDetail d
            join fetch d.booking b
            join fetch b.user u
            join fetch d.roomType r
            where d.qrCodeValue = :token
            """)
    Optional<BookingDetail> findByQrCodeValueForUpdate(@Param("token") String token);

    @Query("""
            select d
            from BookingDetail d
            left join fetch d.roomType
            left join fetch d.room
            where d.booking.id in :bookingIds
            """)
    List<BookingDetail> findByBooking_IdIn(@Param("bookingIds") List<Integer> bookingIds);

    @Query("""
            select d
            from BookingDetail d
            join fetch d.booking b
            join fetch d.roomType r
            where b.id = :bookingId
              and b.user.email = :email
            """)
    Optional<BookingDetail> findBookingDetail(
            @Param("bookingId") Integer bookingId,
            @Param("email") String email);

    @Query("""
            select coalesce(sum(d.quantity), 0)
            from BookingDetail d
            where d.roomType.id = :roomTypeId
              and d.status in :detailStatuses
              and d.booking.status in :bookingStatuses
              and d.expectedCheckIn < :periodEnd
              and d.expectedCheckOut > :periodStart
            """)
    Long sumBookedQuantity(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd,
            @Param("bookingStatuses") Collection<BookingStatus> bookingStatuses,
            @Param("detailStatuses") Collection<String> detailStatuses);

    @Query("""
            select d
            from BookingDetail d
            where d.booking.user.email = :email
            order by d.booking.createdAt desc
            """)
    List<BookingDetail> findBookingHistory(@Param("email") String email);

    @Query("""
            select count(d) > 0
            from BookingDetail d
            where d.room.id = :roomId
              and d.booking.id != :bookingId
              and d.booking.status <> com.example.hotelsmartbookingbackend.enums.BookingStatus.CANCELLED
              and d.expectedCheckIn < :periodEnd
              and d.expectedCheckOut > :periodStart
            """)
    boolean existsOverlappingBookingForRoom(
            @Param("roomId") Integer roomId,
            @Param("bookingId") Integer bookingId,
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd);
}
