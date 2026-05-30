package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

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
    private Roomtype roomtypeid;

    @Size(max = 20)
    @NotNull
    @Column(name = "roomnumber", nullable = false, length = 20)
    private String roomnumber;

    @Column(name = "floornumber")
    private Integer floornumber;

    @Size(max = 100)
    @Column(name = "adminpasscode", length = 100)
    private String adminpasscode;

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
    private Instant createdat;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updatedat", nullable = false)
    private Instant updatedat;

}