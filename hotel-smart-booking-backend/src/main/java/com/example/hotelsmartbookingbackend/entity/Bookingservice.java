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
@Table(name = "bookingservices")
public class BookingService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usageid", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "bookingid", nullable = false)
    private Booking booking;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serviceid", nullable = false)
    private Service service;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "implementedby", nullable = false)
    private User implementedBy;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "unitprice", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Column(name = "totalprice", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "implementedat", nullable = false)
    private Instant implementedAt;

    @Size(max = 500)
    @Column(name = "note", length = 500)
    private String note;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Active'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;
}