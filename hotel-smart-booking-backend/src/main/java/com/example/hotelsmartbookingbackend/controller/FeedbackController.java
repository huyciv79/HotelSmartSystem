package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.FeedbackRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.FeedbackResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Principal principal) {
        String customerEmail = principal.getName();
        FeedbackResponse response = feedbackService.createFeedback(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Gửi đánh giá thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateFeedback(
            @PathVariable Integer id,
            @Valid @RequestBody FeedbackRequest request,
            Principal principal) {
        String customerEmail = principal.getName();
        FeedbackResponse response = feedbackService.updateFeedback(id, request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đánh giá thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(
            @PathVariable Integer id,
            Principal principal) {
        String customerEmail = principal.getName();
        feedbackService.deleteFeedback(id, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedbackById(
            @PathVariable Integer id) {
        FeedbackResponse response = feedbackService.getFeedbackById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đánh giá thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeedbackResponse>>> getFilteredFeedbacks(
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer bookingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<FeedbackResponse> response = feedbackService.getFilteredFeedbacks(roomTypeId, rating, bookingId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", response));
    }

}
