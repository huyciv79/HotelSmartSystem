package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomChangeResponse {

    private Integer bookingId;

    private String bookingReference;

    private String bookingStatus;

    private Integer oldRoomId;

    private String oldRoomNumber;

    private String oldRoomTypeName;

    private Integer newRoomId;

    private String newRoomNumber;

    private Integer newRoomTypeId;

    private String newRoomTypeName;

    private String newRoomPassword;

    private String changeType;

    private BigDecimal priceDifference;

    private BigDecimal newTotalAmount;

    private BigDecimal newFinalAmount;

    private Instant changedAt;

    private String changedByStaff;

    private String reason;

    private String message;
}
