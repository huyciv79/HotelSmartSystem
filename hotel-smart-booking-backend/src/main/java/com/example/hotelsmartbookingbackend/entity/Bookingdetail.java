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
@Table(name = "bookingdetails")
public class BookingDetail {
    @Id
    @Column(name = "bookingid", nullable = false)
    private Integer id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "bookingid", nullable = false)
    private Booking booking;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roomtypeid", nullable = false)
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "roomid")
    private Room room;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "expectedcheckin", nullable = false)
    private Instant expectedCheckIn;

    @NotNull
    @Column(name = "expectedcheckout", nullable = false)
    private Instant expectedCheckOut;

    @Column(name = "actualcheckin")
    private Instant actualCheckIn;

    @Column(name = "actualcheckout")
    private Instant actualCheckOut;

    @NotNull
    @Column(name = "priceatbooking", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtBooking;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "numberofadults", nullable = false)
    private Integer numberOfAdults;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "numberofchildren", nullable = false)
    private Integer numberOfChildren;

    @Size(max = 255)
    @Column(name = "roomkeyaccess")
    private String roomKeyAccess;

    @Column(name = "roomkeygeneratedat")
    private Instant roomKeyGeneratedAt;

    @Column(name = "roomkeyexpiredat")
    private Instant roomKeyExpiredAt;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'NotGenerated'")
    @Column(name = "roomkeystatus", nullable = false, length = 50)
    private String roomKeyStatus = "NotGenerated";

    @Size(max = 255)
    @Column(name = "qrcodevalue")
    private String qrCodeValue;

    @Column(name = "qrcodegeneratedat")
    private Instant qrCodeGeneratedAt;

    @Column(name = "qrcodeexpiredat")
    private Instant qrCodeExpiredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkedinby")
    private User checkedInBy;

    @Column(name = "checkedinat")
    private Instant checkedInAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkedoutby")
    private User checkedOutBy;

    @Column(name = "checkedoutat")
    private Instant checkedOutAt;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Active'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updatedat", nullable = false)
    private Instant updatedAt;
}
