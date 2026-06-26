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
public class InvoiceResponse {
    private Integer bookingId;
    private String bookingReference;
    private String bookingType;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Long nights;
    private String roomTypeName;
    private Integer quantity;
    private List<String> assignedRooms;
    private BigDecimal roomRate;
    private BigDecimal roomTotal;
    private List<ServiceChargeItem> services;
    private BigDecimal serviceTotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private List<PaymentItem> payments;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceChargeItem {
        private Integer usageId;
        private String serviceName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private Instant implementedAt;
        private String note;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentItem {
        private Integer paymentId;
        private BigDecimal amount;
        private String paymentMethod;
        private String paymentType;
        private String transactionCode;
        private String status;
        private BigDecimal refundedAmount;
        private Instant paymentDate;
        private String notes;
    }
}
