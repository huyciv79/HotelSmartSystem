package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, String customerEmail);

    List<BookingHistoryResponse> getBookingHistory(String customerEmail);
}
