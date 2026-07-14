package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.FeedbackImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackImageRepository extends JpaRepository<FeedbackImage, Integer> {
    List<FeedbackImage> findByFeedback_Id(Integer feedbackId);
    void deleteByFeedback_Id(Integer feedbackId);
}
