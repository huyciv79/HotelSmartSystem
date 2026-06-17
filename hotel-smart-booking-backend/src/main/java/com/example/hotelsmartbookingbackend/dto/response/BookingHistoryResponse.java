package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingHistoryResponse {
    private Integer bookingId;
    private String bookingNumber;
    private Instant bookingDate;
    private String checkInMethod;
    private Integer roomTypeId;
    private String roomType;
    private Integer quantity;
    private Integer numberOfAdults;
    private Integer numberOfChildren;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Long nights;
    private BigDecimal totalAmount;
    private String status;
    private String guestName;
    private String guestEmail;
    private Instant actualCheckIn;
    private Instant actualCheckOut;
}
