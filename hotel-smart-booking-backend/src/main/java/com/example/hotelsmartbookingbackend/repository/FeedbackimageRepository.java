package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Feedbackimage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackimageRepository extends JpaRepository<Feedbackimage, Integer> {
    List<Feedbackimage> findByFeedbackid_Id(Integer feedbackId);
    void deleteByFeedbackid_Id(Integer feedbackId);
}
