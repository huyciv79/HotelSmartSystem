package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
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
@Table(name = "customerrequests")
public class Customerrequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requestid", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "bookingid", nullable = false)
    private Booking bookingid;

    @Size(max = 50)
    @NotNull
    @Column(name = "requesttype", nullable = false, length = 50)
    private String requesttype;

    @NotNull
    @Column(name = "description", nullable = false, length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "oldvalue", length = Integer.MAX_VALUE)
    private String oldvalue;

    @Column(name = "newvalue", length = Integer.MAX_VALUE)
    private String newvalue;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Pending'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "resolvedat")
    private Instant resolvedat;

    @Size(max = 500)
    @Column(name = "rejectionreason", length = 500)
    private String rejectionreason;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdat;

}