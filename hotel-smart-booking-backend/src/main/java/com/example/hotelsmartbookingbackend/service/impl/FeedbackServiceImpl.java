package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.FeedbackRequest;
import com.example.hotelsmartbookingbackend.dto.response.FeedbackResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.entity.Feedback;
import com.example.hotelsmartbookingbackend.entity.FeedbackImage;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.entity.BookingDetail;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.BookingDetailRepository;
import com.example.hotelsmartbookingbackend.repository.FeedbackRepository;
import com.example.hotelsmartbookingbackend.repository.FeedbackImageRepository;
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
    private final FeedbackImageRepository feedbackImageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingDetailRepository bookingDetailRepository;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request, String customerEmail) {
        // 1. Kiểm tra booking có tồn tại không
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đặt phòng"));

        // 1b. Kiểm tra giới hạn độ dài bình luận (tối đa 1000 ký tự)
        if (request.getComment() != null && request.getComment().trim().length() > 1000) {
            throw new RuntimeException("Review comment cannot exceed 1000 characters.");
        }

        // 2. Kiểm tra quyền sở hữu booking
        if (!booking.getUser().getEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Bạn không có quyền đánh giá đặt phòng này");
        }

        // 2b. Kiểm tra trạng thái booking đã trả phòng thành công chưa (BR-60)
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá đặt phòng sau khi đã hoàn thành trả phòng (Checked Out)");
        }

        // 3. Kiểm tra xem booking đã được đánh giá chưa
        Optional<Feedback> existing = feedbackRepository.findByBooking_Id(request.getBookingId());
        if (existing.isPresent()) {
            throw new RuntimeException("Đặt phòng này đã được gửi đánh giá trước đó");
        }

        // 4. Tạo bản ghi Feedback
        BookingDetail detail = bookingDetailRepository.findByBooking_Id(booking.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        Feedback feedback = new Feedback();
        feedback.setBooking(booking);
        feedback.setUser(booking.getUser());
        feedback.setRoomType(detail.getRoomType());
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setPros(request.getPros());
        feedback.setCons(request.getCons());
        feedback.setStatus("Active");
        feedback.setCreatedAt(Instant.now());
        feedback.setUpdatedAt(Instant.now());

        Feedback savedFeedback = feedbackRepository.save(feedback);

        // 5. Lưu hình ảnh nếu có
        List<String> imageUrls = new ArrayList<>();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (String url : request.getImages()) {
                if (url != null && !url.isBlank()) {
                    FeedbackImage img = new FeedbackImage();
                    img.setFeedback(savedFeedback);
                    img.setImageUrl(url);
                    img.setUploadedAt(Instant.now());
                    feedbackImageRepository.save(img);
                    imageUrls.add(url);
                }
            }
        }

        return mapToResponse(savedFeedback, booking, booking.getUser(), imageUrls);
    }

    @Override
    @Transactional
    public FeedbackResponse updateFeedback(Integer feedbackId, FeedbackRequest request, String customerEmail) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đánh giá"));

        // Kiểm tra quyền sở hữu đánh giá
        Booking booking = feedback.getBooking();
        if (!booking.getUser().getEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa đánh giá này");
        }

        // Kiểm tra giới hạn độ dài bình luận (tối đa 1000 ký tự)
        if (request.getComment() != null && request.getComment().trim().length() > 1000) {
            throw new RuntimeException("Review comment cannot exceed 1000 characters.");
        }

        // Cập nhật thông tin chính
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setPros(request.getPros());
        feedback.setCons(request.getCons());
        feedback.setUpdatedAt(Instant.now());

        Feedback savedFeedback = feedbackRepository.save(feedback);

        // Xóa ảnh cũ và thêm ảnh mới
        feedbackImageRepository.deleteByFeedback_Id(feedbackId);
        List<String> imageUrls = new ArrayList<>();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (String url : request.getImages()) {
                if (url != null && !url.isBlank()) {
                    FeedbackImage img = new FeedbackImage();
                    img.setFeedback(savedFeedback);
                    img.setImageUrl(url);
                    img.setUploadedAt(Instant.now());
                    feedbackImageRepository.save(img);
                    imageUrls.add(url);
                }
            }
        }

        return mapToResponse(savedFeedback, booking, booking.getUser(), imageUrls);
    }

    @Override
    @Transactional
    public void deleteFeedback(Integer feedbackId, String customerEmail) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đánh giá"));

        User currentUser = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        boolean isOwner = feedback.getBooking().getUser().getEmail().equalsIgnoreCase(customerEmail);
        String roleStr = currentUser.getRole().name();
        boolean isStaffOrManager = "receptionist".equalsIgnoreCase(roleStr) || "manager".equalsIgnoreCase(roleStr);

        if (!isOwner && !isStaffOrManager) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này");
        }

        // Xóa các ảnh liên quan
        feedbackImageRepository.deleteByFeedback_Id(feedbackId);

        // Xóa đánh giá
        feedbackRepository.delete(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(Integer feedbackId) {
        Feedback feedback = feedbackRepository.findByIdWithDetails(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đánh giá"));

        List<FeedbackImage> images = feedbackImageRepository.findByFeedback_Id(feedback.getId());
        List<String> imageUrls = images.stream().map(FeedbackImage::getImageUrl).toList();

        return mapToResponse(feedback, feedback.getBooking(), feedback.getBooking().getUser(), imageUrls);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getMyFeedbacks(String customerEmail) {
        return feedbackRepository.findActiveByCustomerEmail(customerEmail).stream()
                .map(feedback -> {
                    List<String> images = feedbackImageRepository.findByFeedback_Id(feedback.getId()).stream()
                            .map(FeedbackImage::getImageUrl)
                            .toList();
                    return mapToResponse(feedback, feedback.getBooking(), feedback.getBooking().getUser(), images);
                })
                .toList();
    }

    private FeedbackResponse mapToResponse(Feedback feedback, Booking booking, User user, List<String> images) {
        return FeedbackResponse.builder()
                .feedbackId(feedback.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .bookingReference(booking != null ? booking.getBookingReference() : null)
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .pros(feedback.getPros())
                .cons(feedback.getCons())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .customerName(user != null ? user.getFullName() : "Hội viên The Iris")
                .customerEmail(user != null ? user.getEmail() : null)
                .customerAvatar(user != null ? user.getAvatar() : null)
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
            List<FeedbackImage> images = feedbackImageRepository.findByFeedback_Id(f.getId());
            List<String> imageUrls = images.stream().map(FeedbackImage::getImageUrl).toList();
            return mapToResponse(f, f.getBooking(), f.getBooking().getUser(), imageUrls);
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
