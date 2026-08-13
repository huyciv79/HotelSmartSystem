package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roomid", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roomtypeid", nullable = false)
    private RoomType roomType;

    @Size(max = 20)
    @NotNull
    @Column(name = "roomnumber", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "floornumber")
    private Integer floorNumber;

    @Size(max = 100)
    @Column(name = "adminpasscode", length = 100)
    private String adminPasscode;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Available'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Size(max = 500)
    @Column(name = "note", length = 500)
    private String note;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updatedat", nullable = false)
    private Instant updatedAt;

    @NotNull
    @ColumnDefault("2")
    @Column(name = "adultcapacity", nullable = false)
    private Integer adultCapacity;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "childcapacity", nullable = false)
    private Integer childCapacity;

    @Column(name = "totalcapacity")
    private Integer totalCapacity;

    @Column(name = "area")
    private BigDecimal area;

    @Column(name = "bedtype", length = Integer.MAX_VALUE)
    private String bedType;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "bedcount", nullable = false)
    private Integer bedCount;



}