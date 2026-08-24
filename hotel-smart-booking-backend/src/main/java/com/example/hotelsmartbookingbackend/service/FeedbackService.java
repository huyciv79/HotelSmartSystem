package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.FeedbackRequest;
import com.example.hotelsmartbookingbackend.dto.response.FeedbackResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;

import java.util.List;


public interface FeedbackService {

    FeedbackResponse createFeedback(FeedbackRequest request, String customerEmail);

    FeedbackResponse updateFeedback(Integer feedbackId, FeedbackRequest request, String customerEmail);

    void deleteFeedback(Integer feedbackId, String customerEmail);

    FeedbackResponse getFeedbackById(Integer feedbackId);

    List<FeedbackResponse> getMyFeedbacks(String customerEmail);

    PageResponse<FeedbackResponse> getFilteredFeedbacks(Integer roomTypeId, Integer rating, Integer bookingId, int page, int size);
}
