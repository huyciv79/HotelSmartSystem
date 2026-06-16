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
public class Bookingdetail {
    @Id
    @Column(name = "bookingid", nullable = false)
    private Integer id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "bookingid", nullable = false)
    private Booking bookingid;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roomtypeid", nullable = false)
    private Roomtype roomtypeid;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "roomid")
    private Room roomid;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "expectedcheckin", nullable = false)
    private Instant expectedcheckin;

    @NotNull
    @Column(name = "expectedcheckout", nullable = false)
    private Instant expectedcheckout;

    @Column(name = "actualcheckin")
    private Instant actualcheckin;

    @Column(name = "actualcheckout")
    private Instant actualcheckout;

    @NotNull
    @Column(name = "priceatbooking", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceatbooking;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "numberofadults", nullable = false)
    private Integer numberofadults;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "numberofchildren", nullable = false)
    private Integer numberofchildren;

    @Size(max = 255)
    @Column(name = "roomkeyaccess")
    private String roomkeyaccess;

    @Size(max = 255)
    @Column(name = "qrcodevalue")
    private String qrcodevalue;

    @Column(name = "qrcodegeneratedat")
    private Instant qrcodegeneratedat;

    @Column(name = "qrcodeexpiredat")
    private Instant qrcodeexpiredat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkedinby")
    private User checkedinby;

    @Column(name = "checkedinat")
    private Instant checkedinat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkedoutby")
    private User checkedoutby;

    @Column(name = "checkedoutat")
    private Instant checkedoutat;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Active'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdat;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updatedat", nullable = false)
    private Instant updatedat;

}