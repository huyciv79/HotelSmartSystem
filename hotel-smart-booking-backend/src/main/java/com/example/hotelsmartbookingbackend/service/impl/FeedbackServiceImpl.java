package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.FeedbackRequest;
import com.example.hotelsmartbookingbackend.dto.response.FeedbackResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.Feedback;
import com.example.hotelsmartbookingbackend.entity.Feedbackimage;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.FeedbackRepository;
import com.example.hotelsmartbookingbackend.repository.FeedbackimageRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackimageRepository feedbackimageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request, String customerEmail) {
        // 1. Kiểm tra booking có tồn tại không
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đặt phòng"));

        // 2. Kiểm tra quyền sở hữu booking
        if (!booking.getUserid().getEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Bạn không có quyền đánh giá đặt phòng này");
        }

        // 2b. Kiểm tra trạng thái booking đã trả phòng thành công chưa (BR-60)
        String bookingStatus = booking.getStatus();
        if (!"Checked Out".equalsIgnoreCase(bookingStatus) &&
                !"Checked-out".equalsIgnoreCase(bookingStatus) &&
                !"Completed".equalsIgnoreCase(bookingStatus)) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá đặt phòng sau khi đã hoàn thành trả phòng (Checked Out)");
        }

        // 3. Kiểm tra xem booking đã được đánh giá chưa
        Optional<Feedback> existing = feedbackRepository.findByBookingid_Id(request.getBookingId());
        if (existing.isPresent()) {
            throw new RuntimeException("Đặt phòng này đã được gửi đánh giá trước đó");
        }

        // 4. Tạo bản ghi Feedback
        Feedback feedback = new Feedback();
        feedback.setBookingid(booking);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setPros(request.getPros());
        feedback.setCons(request.getCons());
        feedback.setStatus("Active");
        feedback.setCreatedat(Instant.now());
        feedback.setUpdatedat(Instant.now());

        Feedback savedFeedback = feedbackRepository.save(feedback);

        // 5. Lưu hình ảnh nếu có
        List<String> imageUrls = new ArrayList<>();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (String url : request.getImages()) {
                if (url != null && !url.isBlank()) {
                    Feedbackimage img = new Feedbackimage();
                    img.setFeedbackid(savedFeedback);
                    img.setImageurl(url);
                    img.setUploadedat(Instant.now());
                    feedbackimageRepository.save(img);
                    imageUrls.add(url);
                }
            }
        }

        return mapToResponse(savedFeedback, booking, booking.getUserid(), imageUrls);
    }

    @Override
    @Transactional
    public FeedbackResponse updateFeedback(Integer feedbackId, FeedbackRequest request, String customerEmail) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đánh giá"));

        // Kiểm tra quyền sở hữu đánh giá
        Booking booking = feedback.getBookingid();
        if (!booking.getUserid().getEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa đánh giá này");
        }

        // Cập nhật thông tin chính
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setPros(request.getPros());
        feedback.setCons(request.getCons());
        feedback.setUpdatedat(Instant.now());

        Feedback savedFeedback = feedbackRepository.save(feedback);

        // Xóa ảnh cũ và thêm ảnh mới
        feedbackimageRepository.deleteByFeedbackid_Id(feedbackId);
        List<String> imageUrls = new ArrayList<>();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (String url : request.getImages()) {
                if (url != null && !url.isBlank()) {
                    Feedbackimage img = new Feedbackimage();
                    img.setFeedbackid(savedFeedback);
                    img.setImageurl(url);
                    img.setUploadedat(Instant.now());
                    feedbackimageRepository.save(img);
                    imageUrls.add(url);
                }
            }
        }

        return mapToResponse(savedFeedback, booking, booking.getUserid(), imageUrls);
    }

    @Override
    @Transactional
    public void deleteFeedback(Integer feedbackId, String customerEmail) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đánh giá"));

        User currentUser = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        boolean isOwner = feedback.getBookingid().getUserid().getEmail().equalsIgnoreCase(customerEmail);
        String roleStr = currentUser.getRole().name();
        boolean isStaffOrManager = "receptionist".equalsIgnoreCase(roleStr) || "manager".equalsIgnoreCase(roleStr);

        if (!isOwner && !isStaffOrManager) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này");
        }

        // Xóa các ảnh liên quan
        feedbackimageRepository.deleteByFeedbackid_Id(feedbackId);

        // Xóa đánh giá
        feedbackRepository.delete(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(Integer feedbackId) {
        Feedback feedback = feedbackRepository.findByIdWithDetails(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đánh giá"));

        List<Feedbackimage> images = feedbackimageRepository.findByFeedbackid_Id(feedback.getId());
        List<String> imageUrls = images.stream().map(Feedbackimage::getImageurl).toList();

        return mapToResponse(feedback, feedback.getBookingid(), feedback.getBookingid().getUserid(), imageUrls);
    }

    private FeedbackResponse mapToResponse(Feedback feedback, Booking booking, User user, List<String> images) {
        return FeedbackResponse.builder()
                .feedbackId(feedback.getId())
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingreference())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .pros(feedback.getPros())
                .cons(feedback.getCons())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedat())
                .updatedAt(feedback.getUpdatedat())
                .customerName(user.getFullname())
                .customerAvatar(user.getAvatar())
                .images(images)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FeedbackResponse> getFilteredFeedbacks(Integer roomTypeId, Integer rating, Integer bookingId,
            int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbackPage = feedbackRepository.findActiveFeedbacksFiltered(roomTypeId, rating, bookingId,
                pageable);

        List<FeedbackResponse> content = feedbackPage.getContent().stream().map(f -> {
            List<Feedbackimage> images = feedbackimageRepository.findByFeedbackid_Id(f.getId());
            List<String> imageUrls = images.stream().map(Feedbackimage::getImageurl).toList();
            return mapToResponse(f, f.getBookingid(), f.getBookingid().getUserid(), imageUrls);
        }).toList();

        return PageResponse.<FeedbackResponse>builder()
                .content(content)
                .page(feedbackPage.getNumber())
                .size(feedbackPage.getSize())
                .totalElements(feedbackPage.getTotalElements())
                .totalPages(feedbackPage.getTotalPages())
                .first(feedbackPage.isFirst())
                .last(feedbackPage.isLast())
                .build();
    }
}
