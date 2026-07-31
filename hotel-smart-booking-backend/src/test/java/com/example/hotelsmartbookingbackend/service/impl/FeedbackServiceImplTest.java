package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.FeedbackRequest;
import com.example.hotelsmartbookingbackend.dto.response.FeedbackResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.BookingDetail;
import com.example.hotelsmartbookingbackend.entity.Feedback;
import com.example.hotelsmartbookingbackend.entity.FeedbackImage;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.BookingDetailRepository;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.FeedbackImageRepository;
import com.example.hotelsmartbookingbackend.repository.FeedbackRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private FeedbackImageRepository feedbackImageRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    private User customerUser;
    private User staffUser;
    private User managerUser;
    private User otherCustomerUser;
    private Booking completedBooking;
    private BookingDetail defaultBookingDetail;
    private RoomType defaultRoomType;

    @BeforeEach
    void setUp() {
        customerUser = new User();
        customerUser.setId(1);
        customerUser.setEmail("john@example.com");
        customerUser.setFullName("John Doe");
        customerUser.setRole(Role.customer);

        otherCustomerUser = new User();
        otherCustomerUser.setId(2);
        otherCustomerUser.setEmail("other@example.com");
        otherCustomerUser.setFullName("Other Person");
        otherCustomerUser.setRole(Role.customer);

        staffUser = new User();
        staffUser.setId(3);
        staffUser.setEmail("staff@hotel.com");
        staffUser.setFullName("Staff Member");
        staffUser.setRole(Role.receptionist);

        managerUser = new User();
        managerUser.setId(4);
        managerUser.setEmail("manager@hotel.com");
        managerUser.setFullName("Manager Person");
        managerUser.setRole(Role.manager);

        completedBooking = new Booking();
        completedBooking.setId(100);
        completedBooking.setBookingReference("BK20260728001");
        completedBooking.setStatus(BookingStatus.COMPLETED);
        completedBooking.setUser(customerUser);

        defaultRoomType = new RoomType();
        defaultRoomType.setId(1);
        defaultRoomType.setName("Deluxe Suite");

        defaultBookingDetail = new BookingDetail();
        defaultBookingDetail.setId(500);
        defaultBookingDetail.setBooking(completedBooking);
        defaultBookingDetail.setRoomType(defaultRoomType);
    }

    // ==========================================
    // 1. createFeedback Test Cases (UTCID01 - UTCID07, UTCID19 - UTCID20)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful createFeedback for completed booking owned by customer")
    void should_createFeedbackSuccessfully_when_validRequestAndCompletedBooking() {
        FeedbackRequest request = new FeedbackRequest();
        request.setBookingId(100);
        request.setRating(5);
        request.setComment("Great stay!");
        request.setPros("Clean room");
        request.setCons("None");
        request.setImages(List.of("http://storage.com/img1.jpg", "http://storage.com/img2.jpg"));

        when(bookingRepository.findById(100)).thenReturn(Optional.of(completedBooking));
        when(feedbackRepository.findByBooking_Id(100)).thenReturn(Optional.empty());
        when(bookingDetailRepository.findByBooking_Id(100)).thenReturn(Optional.of(defaultBookingDetail));

        Feedback savedFeedback = new Feedback();
        savedFeedback.setId(10);
        savedFeedback.setBooking(completedBooking);
        savedFeedback.setUser(customerUser);
        savedFeedback.setRoomType(defaultRoomType);
        savedFeedback.setRating(5);
        savedFeedback.setComment("Great stay!");
        savedFeedback.setPros("Clean room");
        savedFeedback.setCons("None");
        savedFeedback.setStatus("Active");
        savedFeedback.setCreatedAt(Instant.now());
        savedFeedback.setUpdatedAt(Instant.now());

        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedFeedback);

        FeedbackResponse response = feedbackService.createFeedback(request, "john@example.com");

        assertNotNull(response);
        assertEquals(10, response.getFeedbackId());
        assertEquals(100, response.getBookingId());
        assertEquals("BK20260728001", response.getBookingReference());
        assertEquals(5, response.getRating());
        assertEquals("Great stay!", response.getComment());
        assertEquals(2, response.getImages().size());
        verify(feedbackRepository).save(any(Feedback.class));
        verify(feedbackImageRepository, times(2)).save(any(FeedbackImage.class));
    }

    @Test
    @DisplayName("UTCID02 - Throw exception when booking is not found in DB")
    void should_throwException_when_createFeedbackWithBookingNotFound() {
        FeedbackRequest request = new FeedbackRequest();
        request.setBookingId(999);

        when(bookingRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.createFeedback(request, "john@example.com"));

        assertEquals("Không tìm thấy thông tin đặt phòng", ex.getMessage());
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    @DisplayName("UTCID03 - Throw exception when customer email is not booking owner")
    void should_throwException_when_createFeedbackWithCustomerNotBookingOwner() {
        FeedbackRequest request = new FeedbackRequest();
        request.setBookingId(100);

        when(bookingRepository.findById(100)).thenReturn(Optional.of(completedBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.createFeedback(request, "other@example.com"));

        assertEquals("Bạn không có quyền đánh giá đặt phòng này", ex.getMessage());
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    @DisplayName("UTCID04 - Throw exception when booking status is not COMPLETED")
    void should_throwException_when_createFeedbackWithBookingStatusNotCompleted() {
        Booking checkedInBooking = new Booking();
        checkedInBooking.setId(101);
        checkedInBooking.setUser(customerUser);
        checkedInBooking.setStatus(BookingStatus.CHECKED_IN);

        FeedbackRequest request = new FeedbackRequest();
        request.setBookingId(101);

        when(bookingRepository.findById(101)).thenReturn(Optional.of(checkedInBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.createFeedback(request, "john@example.com"));

        assertEquals("Bạn chỉ có thể đánh giá đặt phòng sau khi đã hoàn thành trả phòng (Checked Out)", ex.getMessage());
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    @DisplayName("UTCID05 - Throw exception when booking already has existing feedback")
    void should_throwException_when_createFeedbackWithExistingFeedback() {
        FeedbackRequest request = new FeedbackRequest();
        request.setBookingId(100);

        when(bookingRepository.findById(100)).thenReturn(Optional.of(completedBooking));
        when(feedbackRepository.findByBooking_Id(100)).thenReturn(Optional.of(new Feedback()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.createFeedback(request, "john@example.com"));

        assertEquals("Đặt phòng này đã được gửi đánh giá trước đó", ex.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID06 - Throw exception when booking detail is not found in DB")
    void should_throwException_when_createFeedbackWithBookingDetailNotFound() {
        FeedbackRequest request = new FeedbackRequest();
        request.setBookingId(100);

        when(bookingRepository.findById(100)).thenReturn(Optional.of(completedBooking));
        when(feedbackRepository.findByBooking_Id(100)).thenReturn(Optional.empty());
        when(bookingDetailRepository.findByBooking_Id(100)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.createFeedback(request, "john@example.com"));

        assertEquals("Không tìm thấy chi tiết đặt phòng", ex.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID07 - Successful createFeedback without images (null image list)")
    void should_createFeedbackSuccessfully_when_requestWithoutImages() {
        FeedbackRequest request = new FeedbackRequest();
        request.setBookingId(100);
        request.setRating(4);
        request.setComment("Good stay");
        request.setImages(null);

        when(bookingRepository.findById(100)).thenReturn(Optional.of(completedBooking));
        when(feedbackRepository.findByBooking_Id(100)).thenReturn(Optional.empty());
        when(bookingDetailRepository.findByBooking_Id(100)).thenReturn(Optional.of(defaultBookingDetail));

        Feedback savedFeedback = new Feedback();
        savedFeedback.setId(10);
        savedFeedback.setBooking(completedBooking);
        savedFeedback.setUser(customerUser);
        savedFeedback.setRoomType(defaultRoomType);
        savedFeedback.setRating(4);
        savedFeedback.setComment("Good stay");

        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedFeedback);

        FeedbackResponse response = feedbackService.createFeedback(request, "john@example.com");

        assertNotNull(response);
        assertEquals(4, response.getRating());
        assertTrue(response.getImages().isEmpty());
        verify(feedbackImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID19 - Successful createFeedback when images list is empty")
    void should_createFeedbackSuccessfully_when_imagesListIsEmpty() {
        FeedbackRequest request = new FeedbackRequest();
        request.setBookingId(100);
        request.setRating(4);
        request.setComment("Good stay");
        request.setImages(List.of()); // Empty list

        when(bookingRepository.findById(100)).thenReturn(Optional.of(completedBooking));
        when(feedbackRepository.findByBooking_Id(100)).thenReturn(Optional.empty());
        when(bookingDetailRepository.findByBooking_Id(100)).thenReturn(Optional.of(defaultBookingDetail));

        Feedback savedFeedback = new Feedback();
        savedFeedback.setId(10);
        savedFeedback.setBooking(completedBooking);
        savedFeedback.setUser(customerUser);
        savedFeedback.setRating(4);

        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedFeedback);

        FeedbackResponse response = feedbackService.createFeedback(request, "john@example.com");

        assertNotNull(response);
        assertTrue(response.getImages().isEmpty());
        verify(feedbackImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID20 - CreateFeedback filters out null and blank image URLs")
    void should_filterOutNullAndBlankImageUrls_when_creatingFeedback() {
        FeedbackRequest request = new FeedbackRequest();
        request.setBookingId(100);
        request.setRating(5);
        List<String> images = new ArrayList<>();
        images.add("  "); // Blank
        images.add(null); // Null
        images.add("http://storage.com/valid.jpg"); // Valid
        request.setImages(images);

        when(bookingRepository.findById(100)).thenReturn(Optional.of(completedBooking));
        when(feedbackRepository.findByBooking_Id(100)).thenReturn(Optional.empty());
        when(bookingDetailRepository.findByBooking_Id(100)).thenReturn(Optional.of(defaultBookingDetail));

        Feedback savedFeedback = new Feedback();
        savedFeedback.setId(10);
        savedFeedback.setBooking(completedBooking);
        savedFeedback.setUser(customerUser);
        savedFeedback.setRating(5);

        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedFeedback);

        FeedbackResponse response = feedbackService.createFeedback(request, "john@example.com");

        assertNotNull(response);
        assertEquals(1, response.getImages().size());
        assertEquals("http://storage.com/valid.jpg", response.getImages().get(0));
        verify(feedbackImageRepository, times(1)).save(any(FeedbackImage.class));
    }

    // ==========================================
    // 2. updateFeedback Test Cases (UTCID08 - UTCID10, UTCID21 - UTCID22)
    // ==========================================

    @Test
    @DisplayName("UTCID08 - Successful updateFeedback for owned feedback")
    void should_updateFeedbackSuccessfully_when_validOwnerAndExistingFeedback() {
        Feedback existingFeedback = new Feedback();
        existingFeedback.setId(10);
        existingFeedback.setBooking(completedBooking);
        existingFeedback.setUser(customerUser);
        existingFeedback.setRating(3);

        FeedbackRequest request = new FeedbackRequest();
        request.setRating(5);
        request.setComment("Updated: Excellent!");
        request.setPros("Quiet");
        request.setCons("Slow WiFi");
        request.setImages(List.of("http://storage.com/new1.jpg"));

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(existingFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(existingFeedback);

        FeedbackResponse response = feedbackService.updateFeedback(10, request, "john@example.com");

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Updated: Excellent!", response.getComment());
        verify(feedbackImageRepository).deleteByFeedback_Id(10);
        verify(feedbackImageRepository, times(1)).save(any(FeedbackImage.class));
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when feedback ID is not found for update")
    void should_throwException_when_updateFeedbackWithFeedbackNotFound() {
        FeedbackRequest request = new FeedbackRequest();
        request.setRating(5);

        when(feedbackRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.updateFeedback(999, request, "john@example.com"));

        assertEquals("Không tìm thấy thông tin đánh giá", ex.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID10 - Throw exception when customer is not the feedback owner during update")
    void should_throwException_when_updateFeedbackWithCustomerNotFeedbackOwner() {
        Feedback existingFeedback = new Feedback();
        existingFeedback.setId(10);
        existingFeedback.setBooking(completedBooking);

        FeedbackRequest request = new FeedbackRequest();
        request.setRating(5);

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(existingFeedback));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.updateFeedback(10, request, "other@example.com"));

        assertEquals("Bạn không có quyền chỉnh sửa đánh giá này", ex.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID21 - Successful updateFeedback when new images list is null")
    void should_updateFeedbackSuccessfully_when_newImagesListIsNull() {
        Feedback existingFeedback = new Feedback();
        existingFeedback.setId(10);
        existingFeedback.setBooking(completedBooking);
        existingFeedback.setUser(customerUser);

        FeedbackRequest request = new FeedbackRequest();
        request.setRating(4);
        request.setImages(null);

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(existingFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(existingFeedback);

        FeedbackResponse response = feedbackService.updateFeedback(10, request, "john@example.com");

        assertNotNull(response);
        assertTrue(response.getImages().isEmpty());
        verify(feedbackImageRepository).deleteByFeedback_Id(10);
        verify(feedbackImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID22 - UpdateFeedback filters out null and blank image URLs")
    void should_filterOutNullAndBlankImageUrls_when_updatingFeedback() {
        Feedback existingFeedback = new Feedback();
        existingFeedback.setId(10);
        existingFeedback.setBooking(completedBooking);
        existingFeedback.setUser(customerUser);

        FeedbackRequest request = new FeedbackRequest();
        request.setRating(4);
        List<String> images = new ArrayList<>();
        images.add("");
        images.add(null);
        images.add("http://storage.com/valid_new.jpg");
        request.setImages(images);

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(existingFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(existingFeedback);

        FeedbackResponse response = feedbackService.updateFeedback(10, request, "john@example.com");

        assertNotNull(response);
        assertEquals(1, response.getImages().size());
        assertEquals("http://storage.com/valid_new.jpg", response.getImages().get(0));
        verify(feedbackImageRepository, times(1)).save(any(FeedbackImage.class));
    }

    // ==========================================
    // 3. deleteFeedback Test Cases (UTCID11 - UTCID15, UTCID23 - UTCID24)
    // ==========================================

    @Test
    @DisplayName("UTCID11 - Successful deleteFeedback when user is booking owner customer")
    void should_deleteFeedbackSuccessfully_when_customerIsBookingOwner() {
        Feedback feedback = new Feedback();
        feedback.setId(10);
        feedback.setBooking(completedBooking);

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(feedback));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));

        feedbackService.deleteFeedback(10, "john@example.com");

        verify(feedbackImageRepository).deleteByFeedback_Id(10);
        verify(feedbackRepository).delete(feedback);
    }

    @Test
    @DisplayName("UTCID12 - Successful deleteFeedback when user is Receptionist staff")
    void should_deleteFeedbackSuccessfully_when_userIsReceptionistStaff() {
        Feedback feedback = new Feedback();
        feedback.setId(10);
        feedback.setBooking(completedBooking);

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(feedback));
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        feedbackService.deleteFeedback(10, "staff@hotel.com");

        verify(feedbackImageRepository).deleteByFeedback_Id(10);
        verify(feedbackRepository).delete(feedback);
    }

    @Test
    @DisplayName("UTCID13 - Successful deleteFeedback when user is Hotel Manager")
    void should_deleteFeedbackSuccessfully_when_userIsHotelManager() {
        Feedback feedback = new Feedback();
        feedback.setId(10);
        feedback.setBooking(completedBooking);

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(feedback));
        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));

        feedbackService.deleteFeedback(10, "manager@hotel.com");

        verify(feedbackImageRepository).deleteByFeedback_Id(10);
        verify(feedbackRepository).delete(feedback);
    }

    @Test
    @DisplayName("UTCID14 - Throw exception when feedback ID is not found for deletion")
    void should_throwException_when_deleteFeedbackWithFeedbackNotFound() {
        when(feedbackRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.deleteFeedback(999, "john@example.com"));

        assertEquals("Không tìm thấy thông tin đánh giá", ex.getMessage());
        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    @DisplayName("UTCID15 - Throw exception when user is unauthorized customer (neither owner nor staff/manager)")
    void should_throwException_when_deleteFeedbackWithUnauthorizedUser() {
        Feedback feedback = new Feedback();
        feedback.setId(10);
        feedback.setBooking(completedBooking);

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(feedback));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherCustomerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.deleteFeedback(10, "other@example.com"));

        assertEquals("Bạn không có quyền xóa đánh giá này", ex.getMessage());
        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    @DisplayName("UTCID23 - Throw exception when user email passed to deleteFeedback is not found in DB")
    void should_throwException_when_userEmailNotFoundDuringDeleteFeedback() {
        Feedback feedback = new Feedback();
        feedback.setId(10);
        feedback.setBooking(completedBooking);

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(feedback));
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.deleteFeedback(10, "nonexistent@example.com"));

        assertEquals("Không tìm thấy người dùng", ex.getMessage());
        verify(feedbackRepository, never()).delete(any());
    }

    @Test
    @DisplayName("UTCID24 - Successful deleteFeedback when staff role is uppercase RECEPTIONIST")
    void should_deleteFeedbackSuccessfully_when_staffRoleIsUppercaseReceptionist() {
        User uppercaseStaff = new User();
        uppercaseStaff.setEmail("staff@hotel.com");
        uppercaseStaff.setRole(Role.receptionist);

        Feedback feedback = new Feedback();
        feedback.setId(10);
        feedback.setBooking(completedBooking);

        when(feedbackRepository.findById(10)).thenReturn(Optional.of(feedback));
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(uppercaseStaff));

        feedbackService.deleteFeedback(10, "staff@hotel.com");

        verify(feedbackImageRepository).deleteByFeedback_Id(10);
        verify(feedbackRepository).delete(feedback);
    }

    // ==========================================
    // 4. getFeedbackById Test Cases (UTCID16 - UTCID17)
    // ==========================================

    @Test
    @DisplayName("UTCID16 - Successful getFeedbackById when feedback exists")
    void should_getFeedbackByIdSuccessfully_when_feedbackExists() {
        Feedback feedback = new Feedback();
        feedback.setId(10);
        feedback.setBooking(completedBooking);
        feedback.setRating(5);
        feedback.setComment("Awesome stay");

        FeedbackImage image1 = new FeedbackImage();
        image1.setImageUrl("http://storage.com/img1.jpg");

        when(feedbackRepository.findByIdWithDetails(10)).thenReturn(Optional.of(feedback));
        when(feedbackImageRepository.findByFeedback_Id(10)).thenReturn(List.of(image1));

        FeedbackResponse response = feedbackService.getFeedbackById(10);

        assertNotNull(response);
        assertEquals(10, response.getFeedbackId());
        assertEquals(5, response.getRating());
        assertEquals(1, response.getImages().size());
        assertEquals("http://storage.com/img1.jpg", response.getImages().get(0));
    }

    @Test
    @DisplayName("UTCID17 - Throw exception when getFeedbackById fails because feedback ID is not found")
    void should_throwException_when_getFeedbackByIdWithFeedbackNotFound() {
        when(feedbackRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackService.getFeedbackById(999));

        assertEquals("Không tìm thấy thông tin đánh giá", ex.getMessage());
    }

    // ==========================================
    // 5. getFilteredFeedbacks Test Cases (UTCID18)
    // ==========================================

    @Test
    @DisplayName("UTCID18 - Successful getFilteredFeedbacks matching filter parameters")
    void should_getFilteredFeedbacksSuccessfully_when_matchingFeedbacksExist() {
        Feedback f1 = new Feedback();
        f1.setId(10);
        f1.setBooking(completedBooking);
        f1.setRating(5);

        Page<Feedback> pageResult = new PageImpl<>(List.of(f1), PageRequest.of(0, 10), 1);

        when(feedbackRepository.findActiveFeedbacksFiltered(eq(1), eq(5), any(), any(Pageable.class))).thenReturn(pageResult);
        when(feedbackImageRepository.findByFeedback_Id(10)).thenReturn(List.of());

        PageResponse<FeedbackResponse> response = feedbackService.getFilteredFeedbacks(1, 5, null, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(10, response.getContent().get(0).getFeedbackId());
        assertEquals(0, response.getPage());
        assertEquals(1, response.getTotalElements());
    }
}
