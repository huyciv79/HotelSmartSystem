package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "surchargepolicies")
public class Surchargepolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "surchargeid", nullable = false)
    private Integer id;

    @Size(max = 50)
    @NotNull
    @Column(name = "surchargetype", nullable = false, length = 50)
    private String surchargetype;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "percentageofroomrate", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageofroomrate;

    @Column(name = "fixedamount", precision = 12, scale = 2)
    private BigDecimal fixedamount;

    @Column(name = "starttime")
    private LocalTime starttime;

    @Column(name = "endtime")
    private LocalTime endtime;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "isactive", nullable = false)
    private Boolean isactive = false;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updatedat", nullable = false)
    private Instant updatedat;

}