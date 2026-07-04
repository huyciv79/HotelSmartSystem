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
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificationid", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid")
    private User user;

    @Size(max = 255)
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @NotNull
    @Column(name = "message", nullable = false, length = Integer.MAX_VALUE)
    private String message;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'System'")
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "referenceid")
    private Integer referenceid;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "isread", nullable = false)
    private Boolean isread = false;

    @Column(name = "readat")
    private Instant readat;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "sentat", nullable = false)
    private Instant sentat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", nullable = false)
    private User user;

}