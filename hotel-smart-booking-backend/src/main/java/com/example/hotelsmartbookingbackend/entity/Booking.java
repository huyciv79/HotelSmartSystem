package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    private User userid;

    @Size(max = 20)
    @NotNull
    @Column(name = "bookingreference", nullable = false, unique = true, length = 20)
    private String bookingreference;

    @Size(max = 50)
    @NotNull
    @Column(name = "bookingtype", nullable = false, length = 50)
    private String bookingtype;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Manual'")
    @Column(name = "checkinmethod", nullable = false, length = 50)
    private String checkinmethod;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "totalamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalamount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "paidamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidamount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "depositamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositamount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "discountamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountamount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "taxamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxamount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "servicechargeamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal servicechargeamount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "finalamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalamount;

    @NotNull
    @ColumnDefault("'Pending'")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BookingStatus status;

    @Size(max = 500)
    @Column(name = "cancellationreason", length = 500)
    private String cancellationreason;

    @Column(name = "cancelledat")
    private Instant cancelledat;

    @Column(name = "specialrequests", length = Integer.MAX_VALUE)
    private String specialrequests;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdat;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updatedat", nullable = false)
    private Instant updatedat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelledby")
    private User cancelledby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdby")
    private User createdby;


}

