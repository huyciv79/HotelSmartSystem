package com.example.hotelsmartbookingbackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFilter {
    private String status;
    private String bookingReference;
    private String guestName;
    private String guestEmail;
    private LocalDate checkInDateFrom;
    private LocalDate checkInDateTo;
    private LocalDate checkOutDateFrom;
    private LocalDate checkOutDateTo;
    private String bookingType;
    private String roomType;
    private Integer roomNumber;
    private Integer page;
    private Integer pageSize;
    private String sortBy;
    private String sortDirection;
}
