package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Bookingdetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingdetailRepository extends JpaRepository<Bookingdetail, Integer> {

        Optional<Bookingdetail> findByBookingid_Id(Integer bookingId);

        boolean existsByQrcodevalue(String qrcodevalue);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        select d
                        from Bookingdetail d
                        join fetch d.bookingid b
                        join fetch b.userid u
                        join fetch d.roomtypeid r
                        where d.qrcodevalue = :token
                        """)
        Optional<Bookingdetail> findByQrcodevalueForUpdate(@Param("token") String token);

        @Query("""
                        select d
                        from Bookingdetail d
                        join fetch d.bookingid b
                        join fetch d.roomtypeid r
                        where b.id = :bookingId
                          and b.userid.email = :email
                        """)
        Optional<Bookingdetail> findBookingDetail(
                        @Param("bookingId") Integer bookingId,
                        @Param("email") String email);

        @Query("""
                        select coalesce(sum(d.quantity), 0)
                        from Bookingdetail d
                        where d.roomtypeid.id = :roomTypeId
                          and d.status in :detailStatuses
                          and d.bookingid.status in :bookingStatuses
                          and d.expectedcheckin < :periodEnd
                          and d.expectedcheckout > :periodStart
                        """)
        Long sumBookedQuantity(
                        @Param("roomTypeId") Integer roomTypeId,
                        @Param("periodStart") Instant periodStart,
                        @Param("periodEnd") Instant periodEnd,
                        @Param("bookingStatuses") Collection<String> bookingStatuses,
                        @Param("detailStatuses") Collection<String> detailStatuses);

        @Query("""
                        select d
                        from Bookingdetail d
                        where d.bookingid.userid.email = :email
                        order by d.bookingid.createdat desc
                        """)
        List<Bookingdetail> findBookingHistory(@Param("email") String email);
}
