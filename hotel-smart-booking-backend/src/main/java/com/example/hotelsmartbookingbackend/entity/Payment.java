package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
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
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paymentid", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "bookingid", nullable = false)
    private Booking booking;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Size(max = 50)
    @NotNull
    @Column(name = "paymentmethod", nullable = false, length = 50)
    private String paymentMethod;

    @Size(max = 50)
    @NotNull
    @Column(name = "paymenttype", nullable = false, length = 50)
    private String paymentType;

    @Size(max = 100)
    @Column(name = "transactioncode", length = 100)
    private String transactionCode;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Pending'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "refundedamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "paymentdate", nullable = false)
    private Instant paymentDate;

    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

}