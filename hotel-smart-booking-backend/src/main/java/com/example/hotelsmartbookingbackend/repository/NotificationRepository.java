package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Notification;
import com.example.hotelsmartbookingbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    Page<Notification> findByUserOrderBySentAtDesc(User user, Pageable pageable);
    
    Page<Notification> findByUserAndIsReadOrderBySentAtDesc(User user, Boolean isRead, Pageable pageable);
    
    List<Notification> findByUserAndIsRead(User user, Boolean isRead);
    
    long countByUserAndIsRead(User user, Boolean isRead);
}
