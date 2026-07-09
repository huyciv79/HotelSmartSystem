package com.example.hotelsmartbookingbackend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BookingStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PAID("Paid"),
    PARTIALLY_PAID("Partially Paid"),
    CHECKED_IN("Checked-in"),
    STAYING("Staying"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BookingStatus fromValue(String value) {
        if (value == null) return null;
        for (BookingStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || 
                (status == CHECKED_IN && "Checked In".equalsIgnoreCase(value)) ||
                (status == COMPLETED && ("Checked Out".equalsIgnoreCase(value) || "Checked-out".equalsIgnoreCase(value))) ||
                status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown booking status value: " + value);
    }
}
