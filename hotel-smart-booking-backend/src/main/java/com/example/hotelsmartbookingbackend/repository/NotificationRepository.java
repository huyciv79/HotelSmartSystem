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
    
    Page<Notification> findByUserOrderBySentatDesc(User user, Pageable pageable);
    
    Page<Notification> findByUserAndIsreadOrderBySentatDesc(User user, Boolean isread, Pageable pageable);
    
    List<Notification> findByUserAndIsread(User user, Boolean isread);
    
    long countByUserAndIsread(User user, Boolean isread);
}
