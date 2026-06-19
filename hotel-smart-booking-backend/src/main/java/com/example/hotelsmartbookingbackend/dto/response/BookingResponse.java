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
public class BookingResponse {
    private Integer bookingId;
    private String bookingReference;
    private String bookingType;
    private String checkInMethod;
    private Integer roomTypeId;
    private String roomTypeName;
    private Integer quantity;
    private Integer numberOfAdults;
    private Integer numberOfChildren;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Long nights;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private BigDecimal paidAmount;
    private BigDecimal depositAmount;
    private String status;
    private String specialRequests;
    private Instant actualCheckIn;
    private Instant actualCheckOut;
    private Instant createdAt;
}
