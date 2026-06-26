package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.WalkInBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.InvoiceResponse;
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
            MultipartFile leftImage,
            MultipartFile rightImage,
            MultipartFile upImage,
            MultipartFile downImage,
            String actorEmail
    );

    BookingResponse performCheckOut(Integer bookingId, String staffEmail);

    List<BookingHistoryResponse> getAllBookingsForStaff(String staffEmail);

    BookingResponse createWalkInBooking(WalkInBookingRequest request, String staffEmail);

    InvoiceResponse getInvoiceDetails(Integer bookingId, String actorEmail);

    void addServiceToBooking(Integer bookingId, AddServiceRequest request, String staffEmail);
}

