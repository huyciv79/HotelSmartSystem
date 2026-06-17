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

    Optional<Feedback> findByBookingid_Id(Integer bookingId);

    @Query("""
            select f
            from Feedback f
            join fetch f.bookingid b
            join fetch b.userid u
            where f.id = :feedbackId
            """)
    Optional<Feedback> findByIdWithDetails(@Param("feedbackId") Integer feedbackId);

    @Query("""
            select f
            from Feedback f
            join fetch f.bookingid b
            join fetch b.userid u
            where b.id in (
                select d.id
                from Bookingdetail d
                where d.roomtypeid.id = :roomTypeId
            )
              and f.status = 'Active'
            order by f.createdat desc
            """)
    List<Feedback> findActiveFeedbacksByRoomType(@Param("roomTypeId") Integer roomTypeId);

    @Query(value = """
            select f
            from Feedback f
            join fetch f.bookingid b
            join fetch b.userid u
            where (:roomTypeId is null or b.id in (
                select d.id
                from Bookingdetail d
                where d.roomtypeid.id = :roomTypeId
            ))
              and (:rating is null or f.rating = :rating)
              and (:bookingId is null or b.id = :bookingId)
              and f.status = 'Active'
            order by f.createdat desc
            """,
            countQuery = """
            select count(f)
            from Feedback f
            where (:roomTypeId is null or f.bookingid.id in (
                select d.id
                from Bookingdetail d
                where d.roomtypeid.id = :roomTypeId
            ))
              and (:rating is null or f.rating = :rating)
              and (:bookingId is null or f.bookingid.id = :bookingId)
              and f.status = 'Active'
            """)
    Page<Feedback> findActiveFeedbacksFiltered(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("rating") Integer rating,
            @Param("bookingId") Integer bookingId,
            Pageable pageable);
}
