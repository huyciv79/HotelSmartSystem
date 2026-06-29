package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
    private String roomNumber;
    private String roomPassword;
    private String roomKeyStatus;
    private Instant roomKeyExpiresAt;
    private List<RoomAccessResponse> roomAccesses;
<<<<<<< HEAD
    private EkycIdentitySummaryResponse ekycIdentity;
=======
    
    // Financial details for lobby operations
    private BigDecimal paidAmount;
    private BigDecimal depositAmount;
    private BigDecimal serviceChargeAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
>>>>>>> 72d1cd4 (feat: add payment status badge (100%/30%) to invoice modal and booking detail page)
}
