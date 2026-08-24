package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    Optional<Feedback> findByBooking_Id(Integer bookingId);

    @Query("""
            select f
            from Feedback f
            join fetch f.booking b
            join fetch b.user u
            where lower(u.email) = lower(:customerEmail)
              and f.status = 'Active'
            order by f.createdAt desc
            """)
    List<Feedback> findActiveByCustomerEmail(@Param("customerEmail") String customerEmail);

    @Query("""
            select f
            from Feedback f
            join fetch f.booking b
            join fetch b.user u
            where f.id = :feedbackId
            """)
    Optional<Feedback> findByIdWithDetails(@Param("feedbackId") Integer feedbackId);



    @Query(value = """
            select f
            from Feedback f
            join fetch f.booking b
            join fetch b.user u
            where (:roomTypeId is null or b.id in (
                select d.id
                from BookingDetail d
                where d.roomType.id = :roomTypeId
            ))
              and (:rating is null or f.rating = :rating)
              and (:bookingId is null or b.id = :bookingId)
              and f.status = 'Active'
            order by f.createdAt desc
            """,
            countQuery = """
            select count(f)
            from Feedback f
            where (:roomTypeId is null or f.booking.id in (
                select d.id
                from BookingDetail d
                where d.roomType.id = :roomTypeId
            ))
              and (:rating is null or f.rating = :rating)
              and (:bookingId is null or f.booking.id = :bookingId)
              and f.status = 'Active'
            """)
    Page<Feedback> findActiveFeedbacksFiltered(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("rating") Integer rating,
            @Param("bookingId") Integer bookingId,
            Pageable pageable);
}
