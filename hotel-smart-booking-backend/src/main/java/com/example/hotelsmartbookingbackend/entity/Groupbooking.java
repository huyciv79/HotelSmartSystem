package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "groupbookings")
public class Groupbooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "groupbookingid", nullable = false)
    private Integer id;

    @Size(max = 200)
    @NotNull
    @Column(name = "groupname", nullable = false, length = 200)
    private String groupname;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "totalrooms", nullable = false)
    private Integer totalrooms;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "totalguests", nullable = false)
    private Integer totalguests;

    @Column(name = "specialrate", precision = 12, scale = 2)
    private BigDecimal specialrate;

    @Size(max = 500)
    @Column(name = "contractfile", length = 500)
    private String contractfile;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Active'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdat;

}