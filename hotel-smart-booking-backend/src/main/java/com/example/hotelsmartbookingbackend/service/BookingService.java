package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
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
            String actorEmail
    );

    BookingResponse performCheckOut(Integer bookingId, String staffEmail);

    List<BookingHistoryResponse> getAllBookingsForStaff(String staffEmail);
}
