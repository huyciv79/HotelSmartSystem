package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.WalkInBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.InvoiceResponse;
import com.example.hotelsmartbookingbackend.dto.request.BookingFilter;
import com.example.hotelsmartbookingbackend.dto.request.UpdateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CancelBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceReadinessResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.QrTokenResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.example.hotelsmartbookingbackend.dto.request.AddServiceRequest;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, String customerEmail);

    BookingResponse createGroupBooking(CreateGroupBookingRequest request, String customerEmail);

    List<BookingHistoryResponse> getBookingHistory(String customerEmail);

    BookingResponse getBookingDetail(Integer bookingId, String customerEmail);

    BookingResponse performCheckIn(Integer bookingId, String staffEmail);

    BookingResponse performFaceCheckIn(
            Integer bookingId,
            MultipartFile selfieImage,
            MultipartFile challengeImage,
            MultipartFile challengeImage2,
            MultipartFile challengeImage3,
            String challengeDirection,
            String actorEmail
    );

    QrTokenResponse generateQrCheckInToken(Integer bookingId, String customerEmail);

    BookingResponse performQrCheckIn(String qrToken, String actorEmail);

    AiFaceReadinessResponse checkFaceReadiness(
            MultipartFile selfieImage,
            String actorEmail
    );

    BookingResponse performCheckOut(Integer bookingId, String staffEmail);

    List<BookingHistoryResponse> getAllBookingsForStaff(String staffEmail);

    BookingResponse createWalkInBooking(WalkInBookingRequest request, String staffEmail);

    InvoiceResponse getInvoiceDetails(Integer bookingId, String actorEmail);

    void addServiceToBooking(Integer bookingId, AddServiceRequest request, String staffEmail);
    PageResponse<BookingHistoryResponse> filterBookings(BookingFilter criteria);

    BookingResponse updateBooking(Integer bookingId, UpdateBookingRequest request, String staffEmail);

    BookingResponse cancelBooking(Integer bookingId, CancelBookingRequest request, String staffEmail);
}

