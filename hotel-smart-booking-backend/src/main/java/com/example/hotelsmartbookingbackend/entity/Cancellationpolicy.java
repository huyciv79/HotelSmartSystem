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
@Table(name = "cancellationpolicies")
public class Cancellationpolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policyid", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "policyname", nullable = false, length = 100)
    private String policyname;

    @NotNull
    @Column(name = "daysbeforecheckin", nullable = false)
    private Integer daysbeforecheckin;

    @NotNull
    @Column(name = "refundpercentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundpercentage;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "isactive", nullable = false)
    private Boolean isactive = false;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "priority", nullable = false)
    private Integer priority;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdat;

}