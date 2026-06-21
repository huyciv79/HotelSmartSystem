package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "bookingroomaccesses")
public class BookingRoomAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accessid", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "bookingid", nullable = false)
    private Booking bookingid;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roomid", nullable = false)
    private Room roomid;

    @Size(max = 255)
    @Column(name = "roomkeyaccess")
    private String roomkeyaccess;

    @NotNull
    @Column(name = "roomkeygeneratedat", nullable = false)
    private Instant roomkeygeneratedat;

    @NotNull
    @Column(name = "roomkeyexpiredat", nullable = false)
    private Instant roomkeyexpiredat;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Active'")
    @Column(name = "roomkeystatus", nullable = false, length = 50)
    private String roomkeystatus;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdat;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updatedat", nullable = false)
    private Instant updatedat;
}
