package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.BookingFilter;
import com.example.hotelsmartbookingbackend.dto.request.UpdateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CancelBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, String customerEmail);

    BookingResponse createGroupBooking(CreateGroupBookingRequest request, String customerEmail);

    List<BookingHistoryResponse> getBookingHistory(String customerEmail);

    BookingResponse getBookingDetail(Integer bookingId, String customerEmail);

    BookingResponse performCheckIn(Integer bookingId, String staffEmail);

    BookingResponse performFaceCheckIn(
            Integer bookingId,
            MultipartFile selfieImage,
            MultipartFile leftImage,
            MultipartFile rightImage,
            MultipartFile upImage,
            MultipartFile downImage,
            String actorEmail
    );

    BookingResponse performCheckOut(Integer bookingId, String staffEmail);

    List<BookingHistoryResponse> getAllBookingsForStaff(String staffEmail);

    PageResponse<BookingHistoryResponse> filterBookings(BookingFilter criteria);

    BookingResponse updateBooking(Integer bookingId, UpdateBookingRequest request, String staffEmail);

    BookingResponse cancelBooking(Integer bookingId, CancelBookingRequest request, String staffEmail);
}
