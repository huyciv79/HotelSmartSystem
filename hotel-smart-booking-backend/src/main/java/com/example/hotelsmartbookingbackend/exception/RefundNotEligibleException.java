package com.example.hotelsmartbookingbackend.exception;

/**
 * Raised when a booking does not qualify for a refund under the cancellation policy.
 */
public class RefundNotEligibleException extends RuntimeException {

    public RefundNotEligibleException(String message) {
        super(message);
    }
}
