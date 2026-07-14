package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;


import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookingid", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid")
    private User user;

    @Size(max = 20)
    @NotNull
    @Column(name = "bookingreference", nullable = false, unique = true, length = 20)
    private String bookingReference;

    @Size(max = 50)
    @NotNull
    @Column(name = "bookingtype", nullable = false, length = 50)
    private String bookingType;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Manual'")
    @Column(name = "checkinmethod", nullable = false, length = 50)
    private String checkInMethod;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "totalamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "paidamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "depositamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "discountamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "taxamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "servicechargeamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal serviceChargeAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "finalamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @NotNull
    @ColumnDefault("'Pending'")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BookingStatus status;

    @Size(max = 500)
    @Column(name = "cancellationreason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelledat")
    private Instant cancelledAt;

    @Column(name = "specialrequests", length = Integer.MAX_VALUE)
    private String specialRequests;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updatedat", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelledby")
    private User cancelledBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdby")
    private User createdBy;


}

