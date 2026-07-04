package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Notification;
import com.example.hotelsmartbookingbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByUserOrderBySentatDesc(User user, Pageable pageable);

    Page<Notification> findByUserAndIsreadOrderBySentatDesc(User user, Boolean isread, Pageable pageable);

    long countByUserAndIsread(User user, Boolean isread);

    List<Notification> findByUserOrderBySentatDesc(User user);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isread = true, n.readat = :readAt WHERE n.user = :user AND n.id = :id")
    void markAsRead(@Param("user") User user, @Param("id") Integer id, @Param("readAt") Instant readAt);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isread = true, n.readat = :readAt WHERE n.user = :user")
    void markAllAsRead(@Param("user") User user, @Param("readAt") Instant readAt);

    @Modifying
    @Transactional
    void deleteByUserAndId(User user, Integer id);
}
